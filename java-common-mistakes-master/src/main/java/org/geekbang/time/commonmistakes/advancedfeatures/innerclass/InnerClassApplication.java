package org.geekbang.time.commonmistakes.advancedfeatures.innerclass;

public class InnerClassApplication {

    private String gender = "male";

    public static void main(String[] args) throws Exception {

        InnerClassApplication application = new InnerClassApplication();
        application.test();
        //zhuye 打印匿名类的属性
        //male  调用匿名内部类的方法

    }

    private void test() {
        MyInnerClass myInnerClass = new MyInnerClass();
        System.out.println(myInnerClass.name);
        myInnerClass.test();
    }

    class MyInnerClass {
        private String name = "zhuye";

        void test() {
            System.out.println(gender);
        }
    }
}
