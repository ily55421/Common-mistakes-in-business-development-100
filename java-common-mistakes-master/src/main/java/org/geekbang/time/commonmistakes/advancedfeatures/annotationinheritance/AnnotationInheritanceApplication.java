package org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * @author 14688
 * 注释是可以被继承的
 */
@Slf4j
public class AnnotationInheritanceApplication {

    public static void main(String[] args) throws NoSuchMethodException {
        wrong();
        right();
        //17:10:27.626 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ParentClass:Class
        //17:10:27.633 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ParentMethod:Method
        //17:10:27.634 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ChildClass:Class
        //17:10:27.634 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ChildMethod:
        //17:10:27.634 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ParentClass:Class
        //17:10:27.634 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ParentMethod:Method
        //17:10:27.700 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ChildClass:Class
        //17:10:27.701 [main] INFO org.geekbang.time.commonmistakes.advancedfeatures.annotationinheritance.AnnotationInheritanceApplication - ChildMethod:Method
    }

    private static String getAnnotationValue(MyAnnotation annotation) {
        if (annotation == null) return "";
        return annotation.value();
    }

    public static void wrong() throws NoSuchMethodException {
        Parent parent = new Parent();
        log.info("ParentClass:{}", getAnnotationValue(parent.getClass().getAnnotation(MyAnnotation.class)));
        log.info("ParentMethod:{}", getAnnotationValue(parent.getClass().getMethod("foo").getAnnotation(MyAnnotation.class)));


        Child child = new Child();
        log.info("ChildClass:{}", getAnnotationValue(child.getClass().getAnnotation(MyAnnotation.class)));
        log.info("ChildMethod:{}", getAnnotationValue(child.getClass().getMethod("foo").getAnnotation(MyAnnotation.class)));

    }

    public static void right() throws NoSuchMethodException {
        Parent parent = new Parent();
        log.info("ParentClass:{}", getAnnotationValue(parent.getClass().getAnnotation(MyAnnotation.class)));
        log.info("ParentMethod:{}", getAnnotationValue(parent.getClass().getMethod("foo").getAnnotation(MyAnnotation.class)));

        Child child = new Child();
        // AnnotatedElementUtils 带注释的元素工具类  findMergedAnnotation 找到合并的注释
        log.info("ChildClass:{}", getAnnotationValue(AnnotatedElementUtils.findMergedAnnotation(child.getClass(), MyAnnotation.class)));
        log.info("ChildMethod:{}", getAnnotationValue(AnnotatedElementUtils.findMergedAnnotation(child.getClass().getMethod("foo"), MyAnnotation.class)));

    }

    @MyAnnotation(value = "Class")
    @Slf4j
    static class Parent {

        @MyAnnotation(value = "Method")
        public void foo() {
        }
    }

    @Slf4j
    static class Child extends Parent {
        @Override
        public void foo() {
        }
    }
}

