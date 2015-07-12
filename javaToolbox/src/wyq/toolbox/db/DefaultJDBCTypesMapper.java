package wyq.toolbox.db;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

/**
 * @author wangyq
 * 
 */
public class DefaultJDBCTypesMapper implements JDBCTypesMapper {

	@Override
	public int getJDBCType(Class<?> c) {
		if (Void.TYPE.equals(c) || Void.class.equals(c)) {
			return Types.NULL;
		} else if (String.class.equals(c)) {
			return Types.VARCHAR;
		} else if (BigDecimal.class.equals(c)) {
			return Types.NUMERIC;
		} else if (Boolean.TYPE.equals(c) || Boolean.class.equals(c)) {
			return Types.BIT;
		} else if (Byte.TYPE.equals(c) || Byte.class.equals(c)) {
			return Types.TINYINT;
		} else if (Short.TYPE.equals(c) || Short.class.equals(c)) {
			return Types.SMALLINT;
		} else if (Integer.TYPE.equals(c) || Integer.class.equals(c)) {
			return Types.INTEGER;
		} else if (Long.TYPE.equals(c) || Long.class.equals(c)) {
			return Types.BIGINT;
		} else if (Float.TYPE.equals(c) || Float.class.equals(c)) {
			return Types.REAL;
		} else if (Double.TYPE.equals(c) || Double.class.equals(c)) {
			return Types.DOUBLE;
		} else if (c.isArray()
				&& (Byte.TYPE.equals(c.getComponentType()) || Byte.class
						.equals(c.getComponentType()))) {
			return Types.VARBINARY;
		} else if (Date.class.equals(c)) {
			return Types.DATE;
		} else if (Time.class.equals(c)) {
			return Types.TIME;
		} else if (Timestamp.class.equals(c)) {
			return Types.TIMESTAMP;
		} else {
			return Types.OTHER;
		}
	}

	private static List<Integer> STR_TYPES = Arrays.asList(Types.VARCHAR,
			Types.CHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR,
			Types.LONGNVARCHAR);

	@Override
	public Class<?> getJavaType(int sqlType) {
		if (sqlType == Types.NULL) {
			return Void.class;
		} else if (isStrTypes(sqlType)) {
			return String.class;
		} else if (sqlType == Types.BIT) {
			return Boolean.class;
		} else if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL) {
			return BigDecimal.class;
		} else if (sqlType == Types.TINYINT) {
			return Byte.class;
		} else if (sqlType == Types.SMALLINT) {
			return Short.class;
		} else if (sqlType == Types.INTEGER) {
			return Integer.class;
		} else if (sqlType == Types.BIGINT) {
			return Long.class;
		} else if (sqlType == Types.REAL || sqlType == Types.FLOAT) {
			return Float.class;
		} else if (sqlType == Types.DOUBLE) {
			return Double.class;
		} else if (sqlType == Types.VARBINARY || sqlType == Types.BINARY) {
			return Byte[].class;
		} else if (sqlType == Types.DATE) {
			return Date.class;
		} else if (sqlType == Types.TIME) {
			return Time.class;
		} else if (sqlType == Types.TIMESTAMP) {
			return Timestamp.class;
		} else if (sqlType == Types.CLOB) {
			return Clob.class;
		} else if (sqlType == Types.BLOB) {
			return Blob.class;
		} else if (sqlType == Types.ARRAY) {
			return Array.class;
		} else if (sqlType == Types.REF) {
			return Ref.class;
		} else if (sqlType == Types.STRUCT) {
			return Struct.class;
		} else {
			return Object.class;
		}
	}

	private boolean isStrTypes(int sqlType) {
		return STR_TYPES.contains(sqlType);
	}

}
