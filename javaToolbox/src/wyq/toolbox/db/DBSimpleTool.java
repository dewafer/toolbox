package wyq.toolbox.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import wyq.toolbox.datamodel.Table;
import wyq.toolbox.datamodel.TableDataSource;
import wyq.toolbox.db.DBEngine.DBResult;

/**
 * This class provides a simple yet elegant way to access the DB. Access the DB
 * using select/insert/update/delete methods without any SQLs. This class is
 * implemented based on the DBEngine. The quote characters in the parameters
 * which may harm the DB is already escaped.
 * 
 * @author dewafer
 * 
 */
public class DBSimpleTool {

	protected DBEngine engine;
	private static final String SQL_PREFIX_INSERT = "INSERT INTO ";
	private static final String SQL_PREFIX_SELECT = "SELECT ";
	private static final String SQL_PREFIX_UPDATE = "UPDATE ";
	private static final String SQL_PREFIX_DELETE = "DELETE FROM ";

	public DBSimpleTool(DBEngine engine) {
		this.engine = engine;
	}

	public int insert(String tblName, List<Map<String, Object>> lines) throws ClassNotFoundException, SQLException {

		engine.connect();

		// prepare SQL prefix
		StringBuilder sqlPrefix = new StringBuilder(SQL_PREFIX_INSERT);
		sqlPrefix.append(sterilizeKey(tblName));

		int resultCount = 0;

		// go with each line
		for (Map<String, Object> entry : lines) {
			// prepare sql
			StringBuilder sql = new StringBuilder(sqlPrefix.toString());
			sql.append(" ( ");
			// key list
			List<String> keyList = new ArrayList<String>();
			Iterator<String> iterator = entry.keySet().iterator();
			while (iterator.hasNext()) {
				String key = sterilizeKey(iterator.next());
				keyList.add(key);
				sql.append(key);
				if (iterator.hasNext()) {
					sql.append(" , ");
				}
			}
			sql.append(" ) VALUES (");
			for (int i = 0; i < keyList.size(); i++) {
				sql.append(" ? ");
				if (i != keyList.size() - 1) {
					sql.append(" , ");
				}
			}
			sql.append(" ) ");
			// set handler
			DBDriverHandler handler = new DBDriverHandler();
			handler.entry = entry;
			handler.keyList = keyList;
			engine.setHandler(handler);
			// go sql
			engine.executeSQL(sql.toString());
			// count
			resultCount += handler.result.getRowsCount();
		}

		engine.close();
		return resultCount;
	}

	public int insert(String tblName, Map<String, Object> values) throws ClassNotFoundException, SQLException {
		List<Map<String, Object>> v = new ArrayList<Map<String, Object>>(1);
		v.add(values);
		return insert(tblName, v);
	}

	public Table select(String tblName, List<String> colList, Map<String, Object> where, List<String> orderBy)
			throws Exception {

		engine.connect();

		// prepare SQL
		StringBuilder sql = new StringBuilder(SQL_PREFIX_SELECT);
		if (colList != null) {
			Iterator<String> itr = colList.iterator();
			while (itr.hasNext()) {
				sql.append(sterilizeKey(itr.next()));
				if (itr.hasNext()) {
					sql.append(" , ");
				}
			}
		} else {
			sql.append(" * ");
		}

		sql.append(" FROM ");
		sql.append(sterilizeKey(tblName));

		List<String> keyList = new ArrayList<String>();
		if (where != null && where.size() > 0) {
			sql.append(" WHERE ");
			Iterator<String> iterator = where.keySet().iterator();
			while (iterator.hasNext()) {
				String key = sterilizeKey(iterator.next());
				keyList.add(key);
				sql.append(key);
				sql.append(" = ?");
				if (iterator.hasNext()) {
					sql.append(" AND ");
				}
			}
		}

		if (orderBy != null && orderBy.size() > 0) {
			sql.append(" ORDER BY ");
			Iterator<String> itr = orderBy.iterator();
			while (itr.hasNext()) {
				sql.append(sterilizeKey(itr.next()));
				if (itr.hasNext()) {
					sql.append(" , ");
				}
			}
		}

		// set handler
		DBDriverHandler handler = new DBDriverHandler();
		handler.entry = where;
		handler.keyList = keyList;
		engine.setHandler(handler);
		// go sql
		engine.executeSQL(sql.toString());
		// process result
		ResultSetTableSourceAdapter adapter = new ResultSetTableSourceAdapter();
		adapter.resultSet = handler.result.getResultSet();
		adapter.metaData = handler.result.getResultSet().getMetaData();

		DBDriverResultTable tbl = new DBDriverResultTable();
		tbl.load(adapter);

		engine.close();

		return tbl;
	}

	public int update(String tblName, List<Map<String, Object>> setList, List<Map<String, Object>> whereList)
			throws SQLException, ClassNotFoundException {

		engine.connect();

		// prepare SQL prefix
		StringBuilder sqlPrefix = new StringBuilder(SQL_PREFIX_UPDATE);
		sqlPrefix.append(sterilizeKey(tblName));
		sqlPrefix.append(" SET ");

		int resultCount = 0;

		for (int i = 0; i < setList.size(); i++) {
			if (i < whereList.size()) {
				StringBuilder sql = new StringBuilder(sqlPrefix.toString());
				DBDriverHandler handler = new DBDriverHandler();

				List<String> keyList = new ArrayList<String>();
				Map<String, Object> mergedEntry = new HashMap<String, Object>();
				Map<String, Object> setEntry = setList.get(i);
				Iterator<String> itr = setEntry.keySet().iterator();
				while (itr.hasNext()) {
					String key = sterilizeKey(itr.next());
					keyList.add(key);
					sql.append(key);
					sql.append(" = ?");
					if (itr.hasNext()) {
						sql.append(" , ");
					}
				}
				mergedEntry.putAll(setEntry);

				Map<String, Object> whereEntry = whereList.get(i);
				if (whereEntry.size() > 0) {
					sql.append(" WHERE ");
					itr = whereEntry.keySet().iterator();
					while (itr.hasNext()) {
						String key = sterilizeKey(itr.next());
						keyList.add(key);
						sql.append(key);
						sql.append(" = ?");
						if (itr.hasNext()) {
							sql.append(" AND ");
						}
					}
					mergedEntry.putAll(whereEntry);
				}
				handler.keyList = keyList;
				handler.entry = mergedEntry;

				engine.setHandler(handler);
				engine.executeSQL(sql.toString());

				resultCount += handler.result.getRowsCount();
			}
		}

		engine.close();
		return resultCount;
	}

	public int update(String tblName, Map<String, Object> set, Map<String, Object> where)
			throws ClassNotFoundException, SQLException {
		List<Map<String, Object>> s = new ArrayList<Map<String, Object>>(1);
		s.add(set);
		List<Map<String, Object>> w = new ArrayList<Map<String, Object>>(1);
		if (where == null) {
			where = new HashMap<String, Object>();
		}
		w.add(where);
		return update(tblName, s, w);
	}

	public int delete(String tblName, List<Map<String, Object>> whereList) throws SQLException, ClassNotFoundException {
		engine.connect();

		// prepare SQL prefix
		StringBuilder sqlPrefix = new StringBuilder(SQL_PREFIX_DELETE);
		sqlPrefix.append(sterilizeKey(tblName));

		int resultCount = 0;

		// go with each line
		for (Map<String, Object> entry : whereList) {
			// prepare sql
			StringBuilder sql = new StringBuilder(sqlPrefix.toString());
			// key list
			List<String> keyList = new ArrayList<String>();
			if (entry.size() > 0) {
				sql.append(" WHERE ");
				Iterator<String> iterator = entry.keySet().iterator();
				while (iterator.hasNext()) {
					String key = sterilizeKey(iterator.next());
					keyList.add(key);
					sql.append(key);
					sql.append(" = ?");
					if (iterator.hasNext()) {
						sql.append(" AND ");
					}
				}
			}
			// set handler
			DBDriverHandler handler = new DBDriverHandler();
			handler.entry = entry;
			handler.keyList = keyList;
			engine.setHandler(handler);
			// go sql
			engine.executeSQL(sql.toString());
			// count
			resultCount += handler.result.getRowsCount();
		}

		engine.close();
		return resultCount;
	}

	public int delete(String tblName, Map<String, Object> where) throws ClassNotFoundException, SQLException {
		List<Map<String, Object>> w = new ArrayList<Map<String, Object>>(1);
		if (where == null) {
			where = new HashMap<String, Object>();
		}
		w.add(where);
		return delete(tblName, w);
	}

	private String sterilizeKey(String key) {
		String escaped = key;
		if (escaped.contains("'")) {
			escaped = escaped.replaceAll("\'", "");
		}
		if (escaped.contains(";")) {
			escaped = escaped.replaceAll(";", "");
		}
		return escaped;
	}

	public DBEngine getEngine() {
		return engine;
	}

	public void setEngine(DBEngine engine) {
		this.engine = engine;
	}

	class DBDriverHandler implements DBEngineHandler {

		List<String> keyList;
		Map<String, Object> entry;
		DBResult result;

		@Override
		public void processResult(DBResult result) {
			this.result = result;
		}

		@Override
		public void prepareParameter(PreparedStatement stmt) throws SQLException {

			if (keyList != null && entry != null) {
				// set values
				for (int i = 0; i < keyList.size(); i++) {
					String key = keyList.get(i);
					Object value = entry.get(key);
					int jdbcType = Types.getJDBCType(value);
					stmt.setObject(i + 1, value, jdbcType);
				}
			}

		}
	}

	class ResultSetTableSourceAdapter implements TableDataSource {

		ResultSet resultSet;
		ResultSetMetaData metaData;

		int currentColDef = 0;
		int currentCol = 0;

		@Override
		public boolean nextColumn() throws Exception {
			if (currentColDef < metaData.getColumnCount()) {
				currentColDef++;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public String getColumnName() throws Exception {
			return metaData.getColumnLabel(currentColDef);
		}

		@Override
		public boolean nextRow() throws Exception {
			currentCol = 0;
			return resultSet.next();
		}

		@Override
		public Class<?> getColumnType() throws Exception {
			return Types.getJavaType(metaData.getColumnType(currentColDef));
		}

		@Override
		public boolean nextRowValue() throws Exception {
			if (currentCol < metaData.getColumnCount()) {
				currentCol++;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Object getRowValue() throws Exception {
			return resultSet.getObject(currentCol, Types.getJavaType(metaData.getColumnType(currentCol)));
		}

	}

	class DBDriverResultTable extends Table {

		public void load(TableDataSource source) throws Exception {
			this.loadData(source);
		}
	}
}
