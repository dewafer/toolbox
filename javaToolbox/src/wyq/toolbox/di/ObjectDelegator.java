package wyq.toolbox.di;

import static wyq.toolbox.util.LogUtils.logIntentionallyIgnoredCatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import wyq.toolbox.util.RetryLookup;
import wyq.toolbox.util.RetryLookup.NotFound;

/**
 * <p>
 * 这个类能为一个接口生成一个代理对象，在这个代理对象上调用的方法会以下面的次序在
 * 执行代理对象上（delegatee）上查找同参数签名的方法，如果找到则执行这个方法并返回结果。
 * <ol>
 * <li>代理接口Class名_方法名</li>
 * <li>方法名</li>
 * <li>如果1和2都没找到，则去父类中找</li>
 * <li>如果父类中没找到，则取父类的父类中找直到Object类</li>
 * </ol>
 * 如果没有在执行代理对象上找到，则按照该方法的返回值，按下列方式返回。
 * <ol>
 * <li>如果返回类型是void，则什么都不做。</li>
 * <li>如果返回类型是一个接口，则用相同的执行代理对象代理这个接口。</li>
 * <li>如果返回对象是基本类型，则尝试返回该基本类型的默认值。</li>
 * <li>如果返回类型是普通类，则尝试实例化这个类，如果无法实例化则返回null。</li>
 * </p>
 * <p>
 * 使用方法请参照{@linkplain wyq.toolbox.di.ObjectDelegatorTest}
 * <p>
 * 
 * @author dewafer
 *
 * @param <T>
 */
public class ObjectDelegator<T> implements InvocationHandler {

	class MethodLookup extends RetryLookup<Method, MethodSignature> {

		public MethodLookup(MethodSignature[] lookups) {
			super(lookups, NOT_NULL_FILTER);
		}

		@Override
		public Method tryLookup(MethodSignature param) {

			Class<?> clazz = param.getTargetClass();
			String methodName = param.getMethodName();
			Class<?>[] arguTypes = param.getArgumentTypes();

			try {
				return clazz.getMethod(methodName, arguTypes);
			} catch (NoSuchMethodException | SecurityException e) {
				// intentionally ignore
				logIntentionallyIgnoredCatch(log, e);
			}

			try {
				return clazz.getDeclaredMethod(methodName, arguTypes);
			} catch (NoSuchMethodException | SecurityException e) {
				// intentionally ignore
				logIntentionallyIgnoredCatch(log, e);
			}

			return null;
		}

	}

	static class MethodSignature {
		private Class<?>[] argumentTypes;
		private String methodName;
		private Class<?> targetClass;

		public MethodSignature(Class<?> targetClass, String methodName, Class<?>[] argumentTypes) {
			this.targetClass = targetClass;
			this.methodName = methodName;
			this.argumentTypes = argumentTypes;
		}

		public Class<?>[] getArgumentTypes() {
			return argumentTypes;
		}

		public String getMethodName() {
			return methodName;
		}

		public Class<?> getTargetClass() {
			return targetClass;
		}

	}

	static Logger log = Logger.getLogger(ObjectDelegator.class.getCanonicalName());

	private static final List<Object> primitiveTypeDefaultValues = Arrays.asList(new Object[] { false,
			Character.UNASSIGNED, (byte) 0, (short) 0, (int) 0, (long) 0, (float) 0, (double) 0 });

	private static final List<Class<?>> primitiveTypes = Arrays.asList(new Class<?>[] { Boolean.TYPE, Character.TYPE,
			Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE });

	public static <U> U delegate(Class<U> delegateInterface, Object delegatee) {
		return new ObjectDelegator<U>(delegateInterface, delegatee).delegate();
	}

	private T proxy;

	private Object target;

	public ObjectDelegator(Class<T> delegateInterface, Object invokeDelegatee) {
		this.target = invokeDelegatee;
		this.proxy = ProxyCreator.newProxy(delegateInterface, this);
	}

	public T delegate() {
		return proxy;
	}

	/**
	 * 根据基本类型获取默认值
	 * 
	 * @param type
	 * @return
	 */
	protected Object getDefaultValueOfPrimitiveType(Class<?> type) {
		return primitiveTypeDefaultValues.get(primitiveTypes.indexOf(type));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (log.isLoggable(Level.FINE)) {
			log.fine("method is invoked:" + method + " with args:" + ((args == null) ? "null" : Arrays.asList(args)));
		}
		Class<?> targetClass = target.getClass();
		String declaringClassName = method.getDeclaringClass().getSimpleName();
		String methodName = method.getName();
		Class<?>[] argTypes = method.getParameterTypes();

		// 先准备需要查找的地方
		List<MethodSignature> lookups = new ArrayList<MethodSignature>();
		lookups.add(new MethodSignature(targetClass, declaringClassName + "_" + methodName, argTypes));
		lookups.add(new MethodSignature(targetClass, methodName, argTypes));
		for (Class<?> parent = targetClass.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
			lookups.add(new MethodSignature(parent, declaringClassName + "_" + methodName, argTypes));
			lookups.add(new MethodSignature(parent, methodName, argTypes));
		}

		// 然后准备查找工具
		MethodLookup lookup = new MethodLookup(lookups.toArray(new MethodSignature[lookups.size()]));

		try {
			// 使用MethodLookup查找，如果找到了则invoke。
			Method targetMethod = lookup.lookup();
			targetMethod.setAccessible(true);
			return targetMethod.invoke(target, args);
		} catch (NotFound | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// intentionally ignore
			logIntentionallyIgnoredCatch(log, e);
		}

		// 返回结果
		// 运行到这里说明没有找到能运行的方法
		Class<?> returnType = method.getReturnType();
		// void
		if (Void.class.equals(returnType)) {
			return null;
		}

		// 接口
		if (returnType.isInterface()) {
			if (returnType.isInstance(proxy)) {
				return proxy;
			} else {
				return ProxyCreator.newProxy(returnType, this);
			}
		}

		// 基本类型
		if (returnType.isPrimitive()) {
			return getDefaultValueOfPrimitiveType(returnType);
		}

		// 通常类
		Object result = null;
		try {
			// 尝试实例化
			result = returnType.newInstance();
		} catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError e) {
			// intentionally ignore
			logIntentionallyIgnoredCatch(log, e);
		}

		return result;

	}

}
