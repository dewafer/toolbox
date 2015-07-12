# toolbox

## 简介

因工作需要写了很多小工具，都放在这里，需要者请自取。

这里的代码都是基于Java SE的，除了测试代码需要用到JUnit外，不需要其他依赖包。

## 使用说明

使用git或GitHub克隆后导入Eclipse即可。

## 常用类及示例

### TextFile

该类扩展自java.io.File并且增加了几对读写方法， 通过这些方法你可以很方便地一次全部读取或者一行行读取一个txt文件的内容。

Example:

```
	// 一次性读取someFile.txt中的所有内容，不需要关闭文件。
	String allTxtContent = new TextFile("someFile.txt").readAll();
```

```
	// 一行一行读取，需要在读取完成后关闭文件。 
	String line = null;
	TextFile txtFile = new TextFile("file.txt");
	while((line = txtFile.readLine()) != null) {
		// process line
		...
	}
	txtFile.close();
```

```
	// 一次性将内容追加到write_to.txt文件末尾，不需要关闭文件。
	TextFile txtFile = new TextFile("write_to.txt");
	txtFile.writeAll(allTxtContent, true); // 设置false来覆盖文件。
```

```
	// 一行一行地写入write_to_line.txt文件，需要在完成后关闭文件
	TextFile txtFile = new TextFile("write_to_line.txt");
	// In line by line writing mode, appending is true by default.
	// And there is no way to change that.
	txtFile.writeLine("one line");
	txtFile.close();
```

### CsvFile

该类扩展自TextFile，能够很方便地利用readAllCsv方法来读取csv整个文件。该方法会返回一个Table对象，然后就可以像操作ResultSet那样操作结果集了，不过请注意这里所有的index都是始于0而不是1的。csv的第一行会被识别为列的头。

Example:

```
	Table csv = new CsvFile("someFile.csv").readAllCsv();
	// next row
	while(csv.next()){
		String nameOfFirstCol = csv.getColumnName(0);
		String valOfFirstCol = (String) csv.getValue(0);
		String sameAsAbove = (String) csv.getValue(nameOfFirstCol);
		// process data
		...
	}
```

Csv文件的写入方法还没想好怎么写……以后再说吧。

### ObjectDelegator

这个类使用java.lang.reflect.Proxy来代理接口，并把在被代理的接口上触发的方法按照一定的规则转发到指定对象上。被指定的对象并不需要实现被代理的接口。更多说明请看测试类ObjectDelegatorTest。

Example:

```
		// ObjectDelegator可以在junit中来mock一些接口，譬如JDBC。
		Connection conn = ObjectDelegator.delegate(Connection.class, this);

		// 这个stmt是一个被代理了的代理对象。
		Statement stmt = conn.createStatement();

		// 这个resultSet也是。
		ResultSet resultSet = stmt.executeQuery("Some data please.");

		// 这个方法会调用this上的ResultSet_getString(String)方法
		String result = resultSet.getString("some value");

		// 然后你会得到"correct value"
		System.out.println(result);
```

this对象上有，this对象并不实现ResultSet：

```
	public String ResultSet_getString(String key) {
		if ("some value".equals(key)) {
			return "correct value";
		} else {
			return "wrong value";
		}
	}
```

### ObjectCreator

这是个然并卵的工具类，给它一个接口，譬如ClassName，它会按照下面的次序查找这个类的实现类并实例化：

		// look up in the following orders
		// 1. package.name.ClassNameImpl
		// 2. package.name.impl.ClassNameImpl
		// 3. package.name.impl.ClassName
		// 4. package.name.DefaultClassName
		// 5. package.name.DefaultHandler

Example:

如果有：

```
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

```
则：

```
		// 如果给它个ObjectCreatorTestIface.class，它会回你个ObjectCreatorTestIfaceImpl的实例。
		// 当然，如果这个实现类没有提供无参构造器，那么你需要指定构造器的参数。
		ObjectCreatorTestIface impl = ObjectCreator.create(ObjectCreatorTestIface.class, "args");
		// Console里面会显示
		// ObjectCreatorTestIfaceImpl#testMethod(args) invoked!
		impl.testMethod();
```

### 其他

可能还有其他类可以用吧，懒得写了请自己看代码吧。
