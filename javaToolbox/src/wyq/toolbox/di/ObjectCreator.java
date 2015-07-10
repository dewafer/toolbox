package wyq.toolbox.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import wyq.toolbox.util.RetryLookup;
import wyq.toolbox.util.RetryLookup.LookupFilter;
import wyq.toolbox.util.RetryLookup.NotFound;

/**
 * This class provides the ability to create objects according to the assigned
 * parameters. If interface instead of class is assigned, the implementation
 * will be looked for in the following orders:
 * <ol>
 * <li>package.name.ClassNameImpl</li>
 * <li>package.name.impl.ClassNameImpl</li>
 * <li>package.name.impl.ClassName</li>
 * <li>package.name.DefaultClassName</li>
 * <li>package.name.DefaultHandler</li>
 * </ol>
 * Created objects will not be cached.
 * 
 * @author dewafer
 * @version 1.2
 *
 * @param <T>
 *            type of the expected object
 */
public class ObjectCreator<T> {

	private Class<T> mClass;
	private Object[] mParams;
	private Constructor<T> constructor;

	public static <R> R create(Class<R> clazz, Object... params) {
		R o = null;
		try {
			// create object
			o = new ObjectCreator<R>(clazz, params).newInstance();
		} catch (Exception e) {
			// throw with runtime
			throw new RuntimeException(e);
		}
		return o;
	}

	public ObjectCreator(Class<T> clazz, Object... params) {

		if (clazz == null) {
			throw new IllegalArgumentException("class can not be null");
		}

		this.mClass = clazz;
		this.mParams = params;
	}

	@SuppressWarnings("unchecked")
	public T newInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ClassNotFoundException {

		if (mClass.isInterface()) {
			// find impl if it's an interface
			Class<T> implClass = (Class<T>) findImpl(mClass);
			if (implClass != null) {
				mClass = implClass;
			} else {
				throw new ClassNotFoundException("interface implementation not found:" + mClass);
			}
		}

		if (this.constructor == null) {
			// find constructor first
			constructor = (Constructor<T>) findConstructor(mClass, mParams);
			if (constructor == null) {
				// constructor not found
				throw new InstantiationException("constructor not found:" + mClass);
			}
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
		}

		// new Object
		T object = (T) constructor.newInstance(mParams);

		return object;
	}

	public static String DEFAULT_PACKAGE_NAME = ObjectCreator.class.getPackage().getName();

	protected Class<?> findImpl(final Class<T> iface) {

		Package pkg = iface.getPackage();
		String pkgName = (pkg != null) ? pkg.getName() : DEFAULT_PACKAGE_NAME;
		String clsName = iface.getSimpleName();
		Class<?> implClass = null;

		// look up in the following orders
		// 1. package.name.ClassNameImpl
		// 2. package.name.impl.ClassNameImpl
		// 3. package.name.impl.ClassName
		// 4. package.name.DefaultClassName
		// 5. package.name.DefaultHandler
		String[] lookups = { pkgName + "." + clsName + "Impl", pkgName + ".impl." + clsName + "Impl",
				pkgName + ".impl." + clsName, pkgName + "." + "Default" + clsName, pkgName + "." + "DefaultHandler" };

		try {
			implClass = new ClassNameLookup(lookups, new LookupFilter<Class<?>>() {

				@Override
				public boolean accept(Class<?> found) {
					return found != null && iface.isAssignableFrom(found);
				}
			}).lookup();
		} catch (NotFound e) {
			// ignore
		}

		return implClass;
	}

	protected Class<?> implLookUp(String classFullName) {
		Class<?> implClass = null;
		try {
			implClass = Class.forName(classFullName);
		} catch (ClassNotFoundException e) {
			// ignore
			e.printStackTrace();
		}
		return implClass;
	}

	protected Constructor<?> findConstructor(Class<?> targetClass, Object[] params) {

		// prepare parameter types
		int paramLength = mParams.length;
		Class<?>[] paramTypes = new Class<?>[paramLength];
		for (int i = 0; i < paramLength; i++) {
			Object v = mParams[i];
			if (v != null) {
				paramTypes[i] = v.getClass();
			} else {
				paramTypes[i] = null;
			}
		}

		try {
			return targetClass.getConstructor(paramTypes);
		} catch (NoSuchMethodException e) {
			// ignore match failure
			e.printStackTrace();
		}

		try {
			return targetClass.getDeclaredConstructor(paramTypes);
		} catch (NoSuchMethodException e) {
			// ignore match failure
			e.printStackTrace();
		}

		// look for each constructor in the declared constructors of the
		// targeted class
		for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
			if (c.getParameterTypes().length != paramTypes.length) {
				// skip if parameter length doesn't match
				continue;
			}
			if (matchAllParamTypes(c, paramTypes)) {
				// return if all parameter types match
				return c;
			}
		}

		// constructor not found
		return null;
	}

	protected boolean matchAllParamTypes(Constructor<?> c, Class<?>[] paramTypes) {
		boolean notMatch = false;
		for (int i = 0; i < c.getParameterTypes().length; i++) {
			Class<?> conParamType = c.getParameterTypes()[i];
			Class<?> paramType = paramTypes[i];
			if (paramType == null) {
				// null parameter always match
				continue;
			}
			if (!equals(conParamType, paramType)) {
				// break if one doesn't match
				notMatch = true;
				break;
			}
		}
		return !notMatch;
	}

	protected boolean equals(Class<?> conParamType, Class<?> paramType) {

		if ((conParamType.isPrimitive() && paramType.isPrimitive())
				|| !(conParamType.isPrimitive() || paramType.isPrimitive())) {
			// return equals if conParamType and paramTypes are all primitive or
			// neither of them are primitive
			return conParamType.equals(paramType);
		} else {

			// if one of conParamType and paramTypes is primitive
			// do auto boxing convert
			conParamType = autoBoxing(conParamType);
			paramType = autoBoxing(paramType);

			if (conParamType.equals(paramType)) {
				return true;
			} else {
				// paramType type can be casted to conParamType
				return conParamType.isAssignableFrom(paramType);
			}

		}

	}

	protected Class<?> autoBoxing(Class<?> c) {
		if (!c.isPrimitive()) {
			return c;
		}
		// 8 elements
		// Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE,
		// Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE
		if (Boolean.TYPE.equals(c)) {
			return Boolean.class;
		} else if (Character.TYPE.equals(c)) {
			return Character.class;
		} else if (Byte.TYPE.equals(c)) {
			return Byte.class;
		} else if (Short.TYPE.equals(c)) {
			return Short.class;
		} else if (Integer.TYPE.equals(c)) {
			return Integer.class;
		} else if (Long.TYPE.equals(c)) {
			return Long.class;
		} else if (Float.TYPE.equals(c)) {
			return Float.class;
		} else if (Double.TYPE.equals(c)) {
			return Double.class;
		} else {
			return c;
		}
	}

	class ClassNameLookup extends RetryLookup<Class<?>, String> {

		public ClassNameLookup(String[] lookups, LookupFilter<Class<?>> filter) {
			super(lookups, filter);
		}

		@Override
		public Class<?> tryLookup(String classFullName) {
			Class<?> implClass = null;
			try {
				implClass = Class.forName(classFullName);
			} catch (ClassNotFoundException e) {
				// ignore
				e.printStackTrace();
			}
			return implClass;
		}

	}

}
