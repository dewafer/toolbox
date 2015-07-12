package wyq.toolbox.di;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import wyq.toolbox.di.ObjectDelegator;

/**
 * 这个类演示了如何使用ObjectDelegator
 * 
 * @author dewafer
 *
 */
public class ObjectDelegatorTest {

	@Before
	public void setUp() {
		// 设置logger
		Logger logger = Logger.getLogger("wyq.toolbox.di");
		Handler h = new ConsoleHandler();
		h.setLevel(Level.ALL);
		logger.addHandler(h);
		logger.setLevel(Level.FINE);// set FINER to see tracing logs
	}

	@Test
	public void testShow() throws Throwable {

		// ObjectDelegator可以用在junit中来mock一些接口，譬如JDBC。
		Connection conn = ObjectDelegator.delegate(Connection.class, this);
		System.out.println("conn:" + conn + " " + Arrays.asList(conn.getClass().getInterfaces()));

		// 这个stmt是一个被代理了的代理对象。
		Statement stmt = conn.createStatement();
		System.out.println("stmt:" + stmt + " " + Arrays.asList(stmt.getClass().getInterfaces()));

		// 这个resultSet也是。
		ResultSet resultSet = stmt.executeQuery("Some data please.");
		System.out.println("resultSet:" + resultSet + " " + Arrays.asList(resultSet.getClass().getInterfaces()));
		
		// conn, stmt, resultSet是3个不同的对象，但是他们都调用了同一个hashCode方法
		// 所以从hashCode的角度来看所有的值都一样。但其实他们并不互相相等。
		System.out.println("conn equals stmt:" + conn.equals(stmt));
		System.out.println("stmt equals resultSet:" + stmt.equals(resultSet));
		System.out.println("resultSet equals conn:" + resultSet.equals(conn));
		
		// 但奇怪的是却和this对象相等，知道为什么吗？
		System.out.println("conn equals this:" + conn.equals(this));
		System.out.println("stmt equals this:" + stmt.equals(this));
		System.out.println("resultSet equals this:" + resultSet.equals(this));

		// 这个方法会调用ResultSet_getString(String)方法
		String result = resultSet.getString("some value");

		// 然后你会得到"correct value"
		System.out.println(result);
		assertEquals("correct value", result);

		// 还可以用来mock request这种...
		HttpServletRequest request = ObjectDelegator.delegate(HttpServletRequest.class, this);

		// 因为servlet的getAttribute是定义在ServletRequest上的，
		// 所以你会得到"this is getAttribute(test)"
		// 而非"this is HttpServletRequest_getAttribute(test)"
		Object value = request.getAttribute("test");
		System.out.println(value);
		assertEquals("this is getAttribute(test)", value);

		// 下面两行的输出是：
		// HttpServletRequest_getHeader(header1)
		// HttpServletRequest_getHeader(header2)
		// 知道为什么吗？
		System.out.println(request.getHeader("header1"));
		System.out.println(((ServletRequest) request).getHeader("header2"));
	}

	public String ResultSet_getString(String key) {
		if ("some value".equals(key)) {
			return "correct value";
		} else {
			return "wrong value";
		}
	}

	public Object getAttribute(String key) {
		return "this is getAttribute(" + key + ")";
	}

	public Object HttpServletRequest_getAttribute(String key) {
		return "this is HttpServletRequest_getAttribute(" + key + ")";
	}

	public String HttpServletRequest_getHeader(String str) {
		return "HttpServletRequest_getHeader(" + str + ")";
	}

	public String ServletRequest_getHeader(String str) {
		return "ServletRequest_getHeader(" + str + ")";
	}

	interface HttpServletRequest extends ServletRequest {
		String getHeader(String str);

		ServletRequest getRequest();
	}

	interface ServletRequest {
		Object getAttribute(String key);

		String getHeader(String str);
	}

}
