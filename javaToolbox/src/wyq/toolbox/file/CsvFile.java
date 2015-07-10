package wyq.toolbox.file;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wyq.toolbox.datamodel.Table;
import wyq.toolbox.datamodel.TableDataSource;

/**
 * This class extends from the TextFile class can read the CSV files through the
 * readAllCsv method and the result is wrapped into a Table object which can be
 * accessed like ResultSet style. TODO: Add methods to write into CSV file.
 * 
 * @author dewafer
 * @version 2
 */
public class CsvFile extends TextFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1970285980252228385L;

	public CsvFile(File parent, String child) {
		super(parent, child);
	}

	public CsvFile(String parent, String child) {
		super(parent, child);
	}

	public CsvFile(URI uri) {
		super(uri);
	}

	public CsvFile(String name) {
		super(name);
	}

	/**
	 * Read all the contents of the csv file and return a
	 * {@linkplain wyq.appengine2.datamodel.Table Table} object.
	 * 
	 * @return {@linkplain wyq.appengine2.datamodel.Table Table}
	 * @throws Exception
	 */
	public Table readAllCsv() throws Exception {
		return new CsvTable(this);
	}

	/**
	 * This class extends {@linkplain wyq.appengine2.datamodel.Table Table}
	 * class and also implements
	 * {@linkplain wyq.appengine2.datamodel.TableDataSource TableDataSource}
	 * interface to provide data for the Table class.
	 * 
	 * @author dewafer
	 * 
	 */
	static class CsvTable extends Table implements TableDataSource {

		private static final char SPLITER_REPLACEMENT = '#';

		private static final String SPLITER = ",";

		private static final String REPLACE_PATTERN = "(^|,)(\"[^\"]*,+[^\"]*\")(,|$)";

		CsvFile file;
		String[] columns;
		String currentLine;
		String[] currentRow;
		int col = -1;
		int row = -1;

		public CsvTable(CsvFile csvFile) throws Exception {
			this.file = csvFile;
			file.reset();
			currentLine = file.readLine();
			columns = lineSpliter(currentLine);
			super.loadData(this);
		}

		private String[] lineSpliter(String line) {
			if (line == null || line.length() == 0)
				return new String[0];
			line = line.replaceAll("\"\"", "");
			Pattern pattern = Pattern.compile(REPLACE_PATTERN);
			Matcher matcher = pattern.matcher(line);
			List<Integer> spliterRepPosList = new ArrayList<Integer>();
			StringBuilder sb = new StringBuilder(line);
			while (matcher.find()) {
				String tmp = matcher.group(2);
				for (int i = 0; i < tmp.length(); i++) {
					if (SPLITER.charAt(0) == tmp.charAt(i)) {
						int pos = matcher.start(2) + i;
						spliterRepPosList.add(pos);
						sb.setCharAt(pos, SPLITER_REPLACEMENT);
					}
				}
			}
			String[] split = sb.toString().split(SPLITER);
			int linepos = 0;
			for (int i = 0; i < split.length; i++) {
				String value = split[i];
				if (value.contains(String.valueOf(SPLITER_REPLACEMENT))) {
					StringBuilder valueSb = new StringBuilder(value);
					for (int j = 0; j < value.length(); j++) {
						if (spliterRepPosList.contains(linepos + j)) {
							valueSb.setCharAt(j, SPLITER.charAt(0));
						}
					}
					split[i] = valueSb.toString();
				}
				linepos += value.length() + 1;
			}
			return split;
		}

		@Override
		public boolean nextColumn() throws Exception {
			return ++col < columns.length;
		}

		@Override
		public String getColumnName() throws Exception {
			return columns[col];
		}

		@Override
		public boolean nextRow() throws Exception {
			boolean next = (currentLine = file.readLine()) != null;
			currentRow = lineSpliter(currentLine);
			row = -1;
			return next;
		}

		@Override
		public Class<?> getColumnType() throws Exception {
			return null;
		}

		@Override
		public boolean nextRowValue() throws Exception {
			return ++row < currentRow.length;
		}

		@Override
		public Object getRowValue() throws Exception {
			return currentRow[row];
		}

	}
}
