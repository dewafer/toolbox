package wyq.toolbox.datamodel;

/**
 * Implement this interface to provide the data for the Table class.
 * 
 * @author dewafer
 * 
 */
public interface TableDataSource {

	boolean nextColumn() throws Exception;

	String getColumnName() throws Exception;

	boolean nextRow() throws Exception;

	Class<?> getColumnType() throws Exception;

	boolean nextRowValue() throws Exception;

	Object getRowValue() throws Exception;

}
