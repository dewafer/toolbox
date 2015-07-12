package wyq.toolbox.di;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class ObjectCreatorTest {

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
	public void showTest() {
		// ObjectCreator是个然并卵的工具类，给它一个接口，他按照下面的次序查找这个类的实现类并实例化：
		// look up in the following orders
		// 1. package.name.ClassNameImpl
		// 2. package.name.impl.ClassNameImpl
		// 3. package.name.impl.ClassName
		// 4. package.name.DefaultClassName
		// 5. package.name.DefaultHandler

		// 所以，如果给它个ObjectCreatorTestIface.class
		// 它会回你个ObjectCreatorTestIfaceImpl的实例。
		// 当然，如果这个实现类没有提供无参构造器，那么你需要指定构造器的参数。
		ObjectCreatorTestIface impl = ObjectCreator.create(ObjectCreatorTestIface.class, "args");
		impl.testMethod();
	}

}

interface ObjectCreatorTestIface {
	public void testMethod();
}

class ObjectCreatorTestIfaceImpl implements ObjectCreatorTestIface {

	private String value;

	public ObjectCreatorTestIfaceImpl(String value) {
		this.value = value;
	}

	@Override
	public void testMethod() {
		System.out.println("ObjectCreatorTestIfaceImpl#testMethod(" + value + ") invoked!");
	}
}
