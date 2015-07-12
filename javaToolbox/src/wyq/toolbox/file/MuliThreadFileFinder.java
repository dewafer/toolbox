package wyq.toolbox.file;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * This class demonstrated the mulit-threads style of file searching.
 * 
 * @author dewafer
 * @version 1
 * 
 */
public class MuliThreadFileFinder {

	public static final long WAIT_TIME_OUT = 800;
	private ExecutorService exec = Executors.newCachedThreadPool();
	private File baseDir;
	private FileFilter condition;
	private FileHandler fileHandler;
	private TerminationMonitor monitor;
	private List<Future<?>> threadPool = Collections
			.synchronizedList(new ArrayList<Future<?>>());

	protected MuliThreadFileFinder(File baseDir, FileFilter condition,
			FileHandler fileHandler) {
		this.baseDir = baseDir;
		this.condition = condition;
		this.fileHandler = fileHandler;
	}

	protected Result search() {
		final Result result;
		if (fileHandler == null) {
			result = new SearchResult();
			fileHandler = new FileHandler() {

				@Override
				public void handle(File f) {
					((SearchResult) result).add(f);
				}

			};
		} else {
			result = new Result();
		}
		SearchThread t = new SearchThread(baseDir);
		Future<?> f = exec.submit(t);
		threadPool.add(f);
		monitor = new TerminationMonitor(result);
		exec.execute(monitor);
		synchronized (monitor) {
			monitor.notifyAll();
		}
		return result;
	}

	/**
	 * This method returns a SearchResult which contains a List of java.io.File
	 * objects match the condition of FileFilter.
	 * 
	 * @param baseDir
	 * @param condition
	 * @return
	 */
	public static SearchResult search(File baseDir, FileFilter condition) {
		return (SearchResult) new MuliThreadFileFinder(baseDir, condition, null)
				.search();
	}

	/**
	 * This method returns a Result which does not contain List but can operate
	 * the java.io.File through the handler whom implemented the FileHandler
	 * interface
	 * 
	 * @param baseDir
	 * @param condition
	 * @param handler
	 * @return
	 */
	public static Result search(File baseDir, FileFilter condition,
			FileHandler handler) {
		return new MuliThreadFileFinder(baseDir, condition, handler).search();
	}

	/**
	 * This class represented the searching Thread.
	 * 
	 * @author dewafer
	 * 
	 */
	class SearchThread implements Runnable {

		private File baseDir;

		@Override
		public void run() {
			try {
				final File[] listFiles = baseDir.listFiles(condition);
				if (listFiles != null && fileHandler != null) {
					if (fileHandler instanceof AsynchronizedFileHandler) {
						exec.execute(new Runnable() {

							@Override
							public void run() {
								for (File f : listFiles) {
									synchronized (fileHandler) {
										fileHandler.handle(f);
									}
								}

							}
						});
					} else {
						for (File f : listFiles) {
							synchronized (fileHandler) {
								fileHandler.handle(f);
							}
						}
					}
				}

				File[] dirs = baseDir.listFiles(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						return arg0.isDirectory();
					}
				});

				if (dirs != null) {
					for (File dir : dirs) {
						SearchThread t = new SearchThread(dir);
						Future<?> f = exec.submit(t);
						threadPool.add(f);
					}
				}
			} catch (RejectedExecutionException e) {
				if (!exec.isShutdown()) {
					throw e;
				}
			} finally {
				synchronized (monitor) {
					monitor.notifyAll();
				}
			}
		}

		public SearchThread(File baseDir) {
			this.baseDir = baseDir;
		}

	}

	/**
	 * The duty of this class is to monitor the status of the Threads.
	 * 
	 * @author dewafer
	 * 
	 */
	class TerminationMonitor implements Runnable {

		private Result result;

		public TerminationMonitor(Result result) {
			this.result = result;
		}

		@Override
		public void run() {
			try {
				do {
					synchronized (this) {
						wait(WAIT_TIME_OUT);
					}
					synchronized (threadPool) {
						List<Future<?>> deadThreads = new ArrayList<Future<?>>();
						for (Future<?> f : threadPool) {
							if (f.isDone()) {
								deadThreads.add(f);
							}
						}
						threadPool.removeAll(deadThreads);
					}
				} while (!threadPool.isEmpty());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				exec.shutdown();
				result.finished();
				synchronized (this) {
					notifyAll();
				}
			}
		}
	}

	/**
	 * This Result can be used to determine the search process is finished or is
	 * still on-going.
	 * 
	 * @author dewafer
	 * 
	 */
	public class Result {

		protected boolean isFinished = false;

		public synchronized boolean isFinished() {
			return isFinished;
		}

		protected synchronized void finished() {
			this.isFinished = true;
		}

		/**
		 * Blocks until all the search threads is finished. This method will not
		 * wait for AsynchronizedFileHandler threads.
		 */
		public void waitFinish() {
			try {
				boolean isDone = isFinished();
				while (!isDone) {
					synchronized (monitor) {
						monitor.wait(WAIT_TIME_OUT);
					}
					isDone = isFinished();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Blocks until all the threads is finished including the
		 * AsynchronizedFileHandler execution threads.
		 */
		public void waitTermination() {
			try {
				while (!exec.isTerminated()) {
					synchronized (monitor) {
						monitor.wait(WAIT_TIME_OUT);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Stop the searching.
		 */
		public void stop() {
			exec.shutdown();
		}

	}

	/**
	 * This SearchResult contains a List of all the files match the condition.
	 * 
	 * @author dewafer
	 * 
	 */
	public class SearchResult extends Result {
		private List<File> result = new ArrayList<File>();

		/**
		 * Get a copy of the real result. While the search is still in progress,
		 * this method will not block and will return a copy of the result which
		 * does not represent the exact result of the searching progress.
		 * 
		 * @return
		 */
		public List<File> getResult() {
			return getResultCopy();
		}

		protected void add(File files) {
			result.add(files);
		}

		/**
		 * This method blocks until the search progress is finished.
		 * 
		 * @return
		 */
		public List<File> getResultAwait() {
			waitFinish();
			return getResultCopy();
		}

		private List<File> getResultCopy() {
			List<File> copy = new ArrayList<File>(result.size());
			copy.addAll(result);
			return copy;
		}
	}

	/**
	 * Implement this interface to handle the file matches the condition. This
	 * handler is executed within the search threads and is synchronized between
	 * the search threads.
	 * 
	 * @author dewafer
	 * 
	 */
	public interface FileHandler {
		public void handle(File f);
	}

	/**
	 * Implement this interface to handle the file. This handler is executed
	 * using a new thread rather than the search threads. And is synchronized
	 * between the execution threads.
	 * 
	 * @author dewafer
	 * 
	 */
	public interface AsynchronizedFileHandler extends FileHandler {

	}
}
