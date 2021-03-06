package wyq.toolbox.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class provides the fundamental access to the DB using the JDBC Driver
 * way. The access process is implemented through the DBEngineHandler and the
 * result is wrapped by the DBResult class.
 * 
 * @author dewafer
 * 
 */
public class DBEngine {

	protected Connection conn;

	protected DBEngineHandler handler;

	protected ConnectionProvider provider;

	public static DBEngine start(String dbDriverClassName, String dbUrl) {
		return start(dbDriverClassName, dbUrl, null, null);
	}

	public static DBEngine start(String dbDriverClassName, String dbUrl, String username, String password) {
		return new DBEngine(new ConnectionProvider() {

			@Override
			public String getDbDriverClassName() {
				return dbDriverClassName;
			}

			@Override
			public String getDbUrlStr() {
				return dbUrl;
			}

			@Override
			public String getUser() {
				return username;
			}

			@Override
			public String getPassword() {
				return password;
			}
		});
	}

	public DBEngine(ConnectionProvider provider) {
		this(provider, null);
	}

	public DBEngine(ConnectionProvider provider, DBEngineHandler handler) {
		this.provider = provider;
		this.handler = handler;
	}

	public void connect() throws ClassNotFoundException, SQLException {
		if (conn == null || conn.isClosed()) {
			Class.forName(provider.getDbDriverClassName());
			conn = DriverManager.getConnection(provider.getDbUrlStr(), provider.getUser(), provider.getPassword());
		}
	}

	public void executeSQL(String sql) throws SQLException {
		if (conn == null)
			return;
		try {
			Statement stmt = null;
			if (sql.contains("?") && handler != null) {
				PreparedStatement pstmt = conn.prepareStatement(sql);
				handlerCall(pstmt);
				stmt = pstmt;
			} else {
				stmt = conn.createStatement();
			}
			if (stmt instanceof PreparedStatement) {
				PreparedStatement pstmt = (PreparedStatement) stmt;
				pstmt.execute();
			} else {
				stmt.execute(sql);
			}
			if (handler != null) {
				ResultSet resultSet = stmt.getResultSet();
				int updateCount = stmt.getUpdateCount();
				DBResult result = new DBResult(updateCount, resultSet);
				handlerCall(result);
			}
			if (!conn.getAutoCommit()) {
				conn.commit();
			}
		} catch (SQLException e) {
			if (!conn.getAutoCommit()) {
				conn.rollback();
			}
		}
	}

	private void handlerCall(Object o) throws SQLException {
		if (handler == null) {
			return;
		}
		if (o instanceof PreparedStatement) {
			PreparedStatement p = (PreparedStatement) o;
			handler.prepareParameter(p);
		} else if (o instanceof DBResult) {
			DBResult r = (DBResult) o;
			handler.processResult(r);
		}
	}

	public void close() throws SQLException {
		if (conn != null && !conn.isClosed()) {
			conn.close();
		}
	}

	public DBEngineHandler getHandler() {
		return handler;
	}

	public void setHandler(DBEngineHandler handler) {
		this.handler = handler;
	}

	public ConnectionProvider getProvider() {
		return provider;
	}

	public class DBResult {
		private int rowsCount;
		private ResultSet resultSet;

		protected DBResult(int rowsCount, ResultSet resultSet) {
			this.rowsCount = rowsCount;
			this.resultSet = resultSet;
		}

		public int getRowsCount() {
			return rowsCount;
		}

		public ResultSet getResultSet() {
			return resultSet;
		}

		public boolean hasResultSet() {
			return this.resultSet != null;
		}

	}

}
