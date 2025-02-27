package fastjsonTest;

import java.util.HashMap;
import java.util.Map;

public class Person {
    private String name;
    private int age;

    HashMap<Object, Object> map;

    {
        System.out.println("static block");
    }

    Person(){
        System.out.println("午餐构造函数");
    }

    Person(String name, int age) {
        System.out.println("有参构造函数");
        this.name = name;
        this.age = age;
    }

    public void setName(String name) {
        System.out.println("set name");
        this.name = name;
    }

    public String getName() {
        System.out.println("get name");
        return name;
    }

    public void setAge(int age) {
        System.out.println("set age");
        this.age = age;
    }

    public int getAge() {
        System.out.println("get age");
        return age;
    }

    public Map getMap() {
        System.out.println("get map");
        return map;
    }
}
