package com.ily55421.synchronize;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.stream.IntStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 16:02
 * @Description TODO 错误五： 代码加锁：不要让“锁”事成为烦心事
 * <p>
 * 正确的做法应该是，为 add 和 compare 都加上方法锁，确保 add 方法执行时，compare 无法读取 a 和 b：
 * public synchronized void add()
 * public synchronized void compare()
 * 所以，使用锁解决问题之前一定要理清楚，我们要保护的是什么逻辑，多线程执行的情况又是怎样的。
 * <p>
 * TODO 01  加锁前要清楚锁和被保护的对象是不是一个层面的  静态字段属于类，类级别的锁才能保护；而非静态字段属于类实例，实例级别的锁就可以保护。
 * 把 wrong 方法定义为静态不就可以了，这个时候锁是类级别的。
 * 可以是可以，
 * TODO 但我们不可能为了解决线程安全问题改变代码结构，把实例方法改为静态方法。
 */
@Slf4j
@RestController
@RequestMapping("interesting")
public class Interesting {
    volatile int a = 1;
    volatile int b = 1;


    @GetMapping("addAndCompare")
    public String addAndCompare() {
        Interesting interesting = new Interesting();
//
//        new Thread(() -> interesting.add()).start();
//
//        new Thread(() -> interesting.compare()).start();
        // 比较的结果 会有线程变化
        //16:04:24.944 [Thread-0] INFO com.ily55421.controller.Interesting - add start
        //16:04:24.944 [Thread-1] INFO com.ily55421.controller.Interesting - compare start
        //16:04:24.946 [Thread-0] INFO com.ily55421.controller.Interesting - add done
        //16:04:24.945 [Thread-1] INFO com.ily55421.controller.Interesting - a:2244,b:2899,true  a:64,b:244,false
        //16:04:24.947 [Thread-1] INFO com.ily55421.controller.Interesting - compare done

        // 加锁之后的加法和比较   不会输出compare 比较结果 值相等
        new Thread(() -> interesting.addSyn()).start();
        new Thread(() -> interesting.compareSyn()).start();
        //16:08:22.583 [Thread-0] INFO com.ily55421.controller.Interesting - add start
        //16:08:22.585 [Thread-0] INFO com.ily55421.controller.Interesting - add done
        //16:08:22.585 [Thread-1] INFO com.ily55421.controller.Interesting - compare start
        //16:08:22.585 [Thread-1] INFO com.ily55421.controller.Interesting - compare done

        return "ok";
    }

    /**
     * 测试 累加  错误
     *
     * @param count
     * @return
     */
    @GetMapping("wrong")
    public int wrong(@RequestParam(value = "count", defaultValue = "1000000") int count) {

        Data.reset();
        //多线程循环一定次数调用Data类不同实例的wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        return Data.getCounter();
        // count=1000000  返回的结果为 223670 因为默认运行 100 万次，所以执行后应该输出 100 万，但页面输出的是 223670
        // 在非静态的 wrong 方法上加锁，只能确保多个线程无法执行同一个实例的 wrong 方法，却不能保证不会执行不同实例的 wrong 方法。
        // 而静态的 counter 在多个实例中共享，所以必然会出现线程安全问题。
    }

    /**
     * 测试 累加  正确
     *
     * @param count
     * @return
     */
    @GetMapping("right")
    public int right(@RequestParam(value = "count",defaultValue = "1000000") int count) {

        Data.reset();
        //多线程循环一定次数调用Data类不同实例的right方法
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().right());
        return Data.getCounter();
        // count=1000000  返回的结果为 1000000
    }

    /**
     * 相加
     */
    public void add() {

        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++;
            b++;
        }
        log.info("add done");
    }

    /**
     * 比较
     */
    public void compare() {

        log.info("compare start");
        for (int i = 0; i < 10000; i++) {
            //a始终等于b吗？
            if (a < b) {
                log.info("a:{},b:{},{}", a, b, a > b);
                //最后的a>b应该始终是false吗？
            }
        }
        log.info("compare done");

    }

    /**
     * 加锁的追加
     */
    public synchronized void addSyn() {

        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++;
            b++;
        }
        log.info("add done");
    }

    /**
     * 加锁的比较
     */
    public synchronized void compareSyn() {

        log.info("compare start");
        for (int i = 0; i < 10000; i++) {
            //a始终等于b吗？
            if (a < b) {
                log.info("a:{},b:{},{}", a, b, a > b);
                //最后的a>b应该始终是false吗？
            }
        }
        log.info("compare done");

    }

    static class Data {

        @Getter
        private static int counter = 0;

        public static int reset() {

            counter = 0;
            return counter;
        }

        /**
         * 错误的加锁 counter是静态成员变量 线程共享
         * 方法示例的锁并不能控制counter
         */
        public synchronized void wrong() {

            counter++;
        }

        /**
         * 创建实例对象对累加 进行加锁控制
         *
         */
        private static Object locker = new Object();

        /**
         * 正确的加锁
         */
        public void right() {
            synchronized (locker) {
                counter++;
            }
        }
    }

}
