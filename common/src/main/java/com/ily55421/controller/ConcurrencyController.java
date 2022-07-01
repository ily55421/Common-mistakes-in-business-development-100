package com.ily55421.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 15:09
 * @Description TODO  没有充分了解并发工具的特性，从而无法发挥其威力
 *  freqs.computeIfAbsent 利用cas替换 synchronized  判断key是否存在
 */
@RestController
@RequestMapping("concurrency")
@Slf4j
public class ConcurrencyController {

    //循环次数
    private static int LOOP_COUNT = 10000000;

    //线程数量
    private static int THREAD_COUNT = 10;

    //元素数量
    private static int ITEM_COUNT = 1000;

    @GetMapping("normalUse")
    private Map<String, Long> normalUse() throws InterruptedException {

        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {

                    //获得一个随机的Key
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    //直接上锁
                    synchronized (freqs) {
                        if (freqs.containsKey(key)) {
                            //Key存在则+1
                            freqs.put(key, freqs.get(key) + 1);
                        } else {
                            //Key不存在则初始化为1
                            freqs.put(key, 1L);

                        }
                    }
                }
        ));
        // 线程终止
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        /// System.out.println(freqs.toString());
        return freqs;
        // 测试结果如下;    key不存在，值也没有初始化为1   但无法充分发挥 ConcurrentHashMap 的威力
        //{
        //  "item115": 10015,
        //  "item357": 9975,
        //}
    }

    /**
     * 利用computeIfAbsent()方法来实例化LongAdder，然后利用LongAdder来进行线程安全计数  优化计数
     * 使用 ConcurrentHashMap 的原子性方法 computeIfAbsent 来做复合逻辑操作，判断 Key 是否存在 Value，如果不存在则把 Lambda 表达式运行后的结果放入 Map 作为 Value，也就是新创建一个 LongAdder 对象，最后返回 Value。
     * <p>
     * 由于 computeIfAbsent 方法返回的 Value 是 LongAdder，是一个线程安全的累加器，因此可以直接调用其 increment 方法进行累加。
     * <p>
     * 这样在确保线程安全的情况下达到极致性能，把之前 7 行代码替换为了 1 行。
     *
     * @return
     * @throws InterruptedException
     */
    @GetMapping("goodUse")
    private Map<String, Long> goodUse() throws InterruptedException {

        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        // THREAD_COUNT 线程数量10
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {

                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);

                    //利用computeIfAbsent()方法来实例化LongAdder，然后利用LongAdder来进行线程安全计数  替换前面的上锁累加
                    // 计算如果不存在 KEY不存在则累加 整个方法调用以原子方式执行，因此每个键最多应用一次该函数。
                    freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
                }
        ));

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        //因为我们的Value是LongAdder而不是Long，所以需要做一次转换才能返回
        return freqs.entrySet().stream()

                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().longValue())
                );
    }

    /**
     * 优化后的代码，相比使用锁来操作 ConcurrentHashMap 的方式，性能提升了 10 倍。
     * computeIfAbsent 为什么如此高效呢？
     * 使用了 Java 自带的 Unsafe 实现的 CAS。它在虚拟机层面确保了写入数据的原子性，比加锁的效率高得多
     * @return
     * @throws InterruptedException
     */
    @GetMapping("good")
    public String good() throws InterruptedException {
        // 简单的秒表，允许对多个任务进行计时，显示每个命名任务的总运行时间和运行时间。
        StopWatch stopWatch = new StopWatch();

        stopWatch.start("normalUse");
        Map<String, Long> normalUse = normalUse();
        stopWatch.stop();

        //校验元素数量
        Assert.isTrue(normalUse.size() == ITEM_COUNT, "normalUse size error");

        //校验累计总数
        Assert.isTrue(normalUse.entrySet().stream()
                        .mapToLong(item -> item.getValue()).reduce(0, Long::sum) == LOOP_COUNT
                , "normalUse count error");
        stopWatch.start("goodUse");

        Map<String, Long> goodUse = goodUse();
        stopWatch.stop();
        Assert.isTrue(goodUse.size() == ITEM_COUNT, "goodUse size error");
        Assert.isTrue(goodUse.entrySet().stream()
                        .mapToLong(item -> item.getValue())
                        .reduce(0, Long::sum) == LOOP_COUNT
                , "goodUse count error");

        // prettyPrint() 生成一个带有描述所有执行任务的表格的字符串。
        log.info(stopWatch.prettyPrint());

        return "OK";
        // 测试结果 运行时间如下
        //ns         %     Task name
        //4429129000  087%  normalUse
        //632962400  013%  goodUse

    }
}
