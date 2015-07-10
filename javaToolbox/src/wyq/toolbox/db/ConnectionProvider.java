package wyq.toolbox.db;


/**
 * This interface provide the least information a Connection needs.
 * 
 * @author dewafer
 * 
 */
public interface ConnectionProvider {
	public abstract String getSqlConnProviderClass();

	public abstract String getConnStr();

	public abstract String getUser();

	public abstract String getPassword();
}
