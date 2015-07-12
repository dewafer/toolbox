package wyq.toolbox.db;


/**
 * This interface provide the least information a Connection needs.
 * 
 * @author dewafer
 * 
 */
public interface ConnectionProvider {
	public abstract String getDbDriverClassName();

	public abstract String getDbUrlStr();

	public abstract String getUser();

	public abstract String getPassword();
}
