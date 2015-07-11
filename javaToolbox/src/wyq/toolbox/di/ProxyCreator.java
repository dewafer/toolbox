package wyq.toolbox.di;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the ability to create dynamic proxy objects.
 * 
 * @author dewafer
 *
 */
public class ProxyCreator {

	static Logger log = Logger.getLogger(ProxyCreator.class.getCanonicalName());

	static ProxyCreator proxyCreator = new ProxyCreator();

	public static Object newProxy(Class<?>[] ifaces, InvocationHandler handler) {
		return proxyCreator.delegate(ifaces, handler);
	}

	@SuppressWarnings("unchecked")
	public static <T> T newProxy(Class<T> iface, InvocationHandler handler) {
		return (T) newProxy(new Class<?>[] { iface }, handler);
	}

	protected Object delegate(Class<?>[] ifaces, InvocationHandler handler) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("new proxy object created: ifaces=" + Arrays.asList(ifaces) + " handler:" + handler);
		}
		return Proxy.newProxyInstance(handler.getClass().getClassLoader(), ifaces, handler);

	}

}
