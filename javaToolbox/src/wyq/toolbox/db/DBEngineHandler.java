package wyq.toolbox.db;

import java.sql.PreparedStatement;

import wyq.toolbox.db.DBEngine.DBResult;

/**
 * implement this interface to access the DB with DBEngine.
 * 
 * @author dewafer
 * 
 */
public interface DBEngineHandler {

	public abstract void prepareParameter(PreparedStatement stmt);

	public abstract void processResult(DBResult result);

}
