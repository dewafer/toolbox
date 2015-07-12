/**
 * 
 */
package wyq.toolbox.db;


/**
 * @author wangyq
 * 
 */
public interface JDBCTypesMapper {

	public int getJDBCType(Class<?> c);

	public Class<?> getJavaType(int sqlType);
}
