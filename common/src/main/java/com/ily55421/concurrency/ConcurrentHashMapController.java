package com.ily55421.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 14:55
 * @Description TODO   并发工具类的线程安全问题
 * todo 错误二：  使用了 ConcurrentHashMap，不代表对它的多个操作之间的状态是一致的，是没有其他线程在操作它的，如果需要确保需要手动加锁。
 */
@RestController
@RequestMapping("concurrent")
@Slf4j
public class ConcurrentHashMapController {
    //线程个数

    private static int THREAD_COUNT = 10;

    //总元素数量

    private static int ITEM_COUNT = 1000;

    //帮助方法，用来获得一个指定元素数量模拟数据的ConcurrentHashMap

    private ConcurrentHashMap<String, Long> getData(int count) {

        return LongStream.rangeClosed(1, count)

                .boxed()
                .collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().toString(), Function.identity(),
                        (o1, o2) -> o1, ConcurrentHashMap::new));

    }

    /**
     * ConcurrentHashMap  错误的ConcurrentHashMap并发示例
     * 多线程下也会出现数据不一致问题
     * @return
     * @throws InterruptedException
     */
    @GetMapping("wrong")
    public String wrong() throws InterruptedException {
        // 创建线程安全map
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);

        //初始900个元素
        log.info("init size:{}", concurrentHashMap.size());
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        //使用线程池并发处理逻辑
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {

            //查询还需要补充多少个元素
            int gap = ITEM_COUNT - concurrentHashMap.size();
            log.info("gap size:{}", gap);

            //补充元素
            concurrentHashMap.putAll(getData(gap));

        }));

        //等待所有任务完成  关闭
        forkJoinPool.shutdown();
        // 等待终止
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        //最后元素个数会是1000吗？
        log.info("finish size:{}", concurrentHashMap.size());

        return "OK";
        //: init size:900
        //2022-07-01 14:58:51.803  INFO 34104 --- [Pool-1-worker-9] c.i.c.ConcurrentHashMapController        : gap size:100
        //2022-07-01 14:58:51.803  INFO 34104 --- [Pool-1-worker-4] c.i.c.ConcurrentHashMapController        : gap size:100
        //2022-07-01 14:58:51.804  INFO 34104 --- [ool-1-worker-13] c.i.c.ConcurrentHashMapController        : gap size:93
        //2022-07-01 14:58:51.804  INFO 34104 --- [ool-1-worker-11] c.i.c.ConcurrentHashMapController        : gap size:92
        //2022-07-01 14:58:51.804  INFO 34104 --- [Pool-1-worker-2] c.i.c.ConcurrentHashMapController        : gap size:92
        //2022-07-01 14:58:51.804  INFO 34104 --- [Pool-1-worker-6] c.i.c.ConcurrentHashMapController        : gap size:88
        //2022-07-01 14:58:51.804  INFO 34104 --- [Pool-1-worker-8] c.i.c.ConcurrentHashMapController        : gap size:88
        //2022-07-01 14:58:51.804  INFO 34104 --- [ool-1-worker-15] c.i.c.ConcurrentHashMapController        : gap size:88
        //2022-07-01 14:58:51.804  INFO 34104 --- [ool-1-worker-10] c.i.c.ConcurrentHashMapController        : gap size:69
        //2022-07-01 14:58:51.804  INFO 34104 --- [Pool-1-worker-1] c.i.c.ConcurrentHashMapController        : gap size:78
        //2022-07-01 14:58:51.807  INFO 34104 --- [nio-8080-exec-1] c.i.c.ConcurrentHashMapController        : finish size:1788

        // 从日志中可以看到：
        //初始大小 900 符合预期，还需要填充 100 个元素。
        //worker13 线程查询到当前需要填充的元素为 93，竟然还不是 100 的倍数。
        //worker11 线程查询到当前需要填充的元素为 93。
        //最后 HashMap 的总项目数是 1788，显然不符合填充满 1000 的预期。

    }

    /**
     * 正确的ConcurrentHashMap 示例
     * @return
     * @throws InterruptedException
     */
    @GetMapping("right")
    public String right() throws InterruptedException {

        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        log.info("init size:{}", concurrentHashMap.size());
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {

            //下面的这段复合逻辑需要锁一下这个ConcurrentHashMap   累减上锁
            synchronized (concurrentHashMap) {
                int gap = ITEM_COUNT - concurrentHashMap.size();
                log.info("gap size:{}", gap);
                concurrentHashMap.putAll(getData(gap));
            }
        }));

        forkJoinPool.shutdown();
        // 等待一秒后 等待终止  TimeUnit.HOURS 截止时间
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        log.info("finish size:{}", concurrentHashMap.size());
        return "OK";
        // 正确输出结果
        //2022-07-01 15:06:06.721  INFO 34104 --- [nio-8080-exec-5] c.i.c.ConcurrentHashMapController        : init size:900
        //2022-07-01 15:06:06.723  INFO 34104 --- [Pool-2-worker-9] c.i.c.ConcurrentHashMapController        : gap size:100
        //2022-07-01 15:06:06.724  INFO 34104 --- [ool-2-worker-10] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [Pool-2-worker-6] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [Pool-2-worker-1] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [Pool-2-worker-4] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [ool-2-worker-13] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [ool-2-worker-15] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.724  INFO 34104 --- [Pool-2-worker-8] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.725  INFO 34104 --- [ool-2-worker-11] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.725  INFO 34104 --- [Pool-2-worker-2] c.i.c.ConcurrentHashMapController        : gap size:0
        //2022-07-01 15:06:06.725  INFO 34104 --- [nio-8080-exec-5] c.i.c.ConcurrentHashMapController        : finish size:1000
    }
}
