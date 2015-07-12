package wyq.toolbox.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogUtils {

	public static void logIntentionallyIgnoredCatch(Logger log, Throwable e) {
		if (!log.isLoggable(Level.FINER)) {
			return;
		}
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		log.finer(w.toString());
	}

}
