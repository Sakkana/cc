package fastjsonTest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class fastjsonTest   {
    public static void main(String[] args) {
        test1();
        test2();
        test3();
    }

    public static void test1() {
        System.out.println("========== Test 1 ==========");
        String s = "{\"param1\": \"111\", \"param2\": \"222\"}";
        JSONObject json = JSONObject.parseObject(s);
        System.out.println(json);
        System.out.println(json.getString("param1"));
    }

    public static void test2() {
        System.out.println("========== Test 2 ==========");

        System.out.println("========== Test 2.1 ==========");
        Person personTom = new Person("Tom", 21);
        String jsonTom = JSONObject.toJSONString(personTom);
        System.out.println(jsonTom);

        System.out.println("========== Test 2.2 ==========");
        String s = "{\"name\": \"Alice\", \"age\": \"18\"}";
        Person jsonObject = JSON.parseObject(s, Person.class);
    }

    public static void test3() {
        System.out.println("========== Test 3 ==========");
        String s = "{\"@type\": \"fastjsonTest.Person\", \"name\": \"Alice\", \"age\": \"18\"}";
        JSONObject jsonObject = JSON.parseObject(s);

        Person person = JSON.parseObject(jsonObject.toJSONString(), Person.class);
        System.out.println(jsonObject);
        System.out.println(person);

        // 会调用两次 getMap，如果是 get only 的 field
        String s2 = "{\"@type\": \"fastjsonTest.Person\", \"name\": \"Alice\", \"age\": \"18\", \"map\": {}}";
        JSONObject jsonObject2 = JSON.parseObject(s2);
    }
}


