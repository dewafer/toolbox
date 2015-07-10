package wyq.toolbox.datamodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract class extends AbstractTable and also implements
 * <code>Iterable</code> interface. Extends this class and pass a
 * {@linkplain wyq.toolbox.datamodel.TableDataSource TableDataSource}
 * implementation to
 * {@linkplain wyq.toolbox.datamodel.Table#loadData(TableDataSource)
 * loadData(TableDataSource)} method to start. See
 * {@link wyq.toolbox.file.CsvFile.CsvTable} for example.
 * 
 * @author dewafer
 * @version 2
 */
public abstract class Table extends AbstractTable<TableDataSource> implements
		Iterable<Map<String, Object>> {

	/**
	 * This method overrides
	 * {@linkplain wyq.appengine2.datamodel.AbstractTable#loadData(T)
	 * super.loadData} to load the data into memory and also make this class
	 * iterable.
	 */
	@Override
	protected void loadData(TableDataSource dataSource) throws Exception {
		while (dataSource.nextColumn()) {
			columnNames.add(dataSource.getColumnName());
			columnTypes.add(dataSource.getColumnType());
		}
		while (dataSource.nextRow()) {
			List<Object> dataRow = new ArrayList<Object>();
			while (dataSource.nextRowValue()) {
				dataRow.add(dataSource.getRowValue());
			}
			resultList.add(dataRow);
		}
	}

	@Override
	public Iterator<Map<String, Object>> iterator() {
		beforeFirst();
		return new Iterator<Map<String, Object>>() {

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Map<String, Object> next() {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				if (Table.this.next()) {
					for (int i = 0; i < Table.this.getColumnCount(); i++) {
						String key = Table.this.getColumnName(i);
						Object value = Table.this.getValue(i);
						row.put(key, value);
					}
				}
				return row;
			}

			@Override
			public boolean hasNext() {
				return !Table.this.isLast();
			}
		};
	}

	@Override
	public String toString() {
		String newLine = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder(super.toString() + "\\DATA=["
				+ newLine);
		Iterator<Map<String, Object>> iterator = this.iterator();
		while (iterator.hasNext()) {
			Map<String, Object> entry = iterator.next();
			Iterator<String> iterator2 = entry.keySet().iterator();
			while (iterator2.hasNext()) {
				String key = iterator2.next();
				Object objValue = entry.get(key);
				sb.append("[" + key + "=" + objValue + "]");
			}
			sb.append(System.getProperty("line.separator"));
		}
		sb.append("]");
		return sb.toString();
	}
}
