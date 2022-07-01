package com.ily55421.synchronize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 16:40
 * @Description TODO 加锁要考虑锁的粒度和场景问题
 * TODO 同样是 1000 次业务操作，正确加锁的版本耗时 1.4 秒，而对整个业务逻辑加锁的话耗时 11 秒。
 * <p>
 * 对于读写比例差异明显的场景，考虑使用 ReentrantReadWriteLock 细化区分读写锁，来提高性能。
 * 如果你的 JDK 版本高于 1.8、共享资源的冲突概率也没那么大的话，考虑使用 StampedLock 的乐观读的特性，进一步提高性能。
 * JDK 里 ReentrantLock 和 ReentrantReadWriteLock 都提供了公平锁的版本，在没有明确需求的情况下不要轻易开启公平锁特性，在任务很轻的情况下开启公平锁可能会让性能下降上百倍。
 */
@Slf4j
@RestController
@RequestMapping("sync")
public class SyncController {

    private List data = new ArrayList<>();

    /**
     * 不涉及共享资源的慢方法
     */
    private void slow() {

        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    /**
     * 错误的加锁方法
     *
     * @return
     */
    @GetMapping("wrong")
    public int wrong() {

        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {

            //加锁粒度太粗了
            synchronized (this) {
                slow();
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
        //took:30675
        //took:18785
        // 响应超时

    }

    /**
     * 正确的加锁方法
     */
    @GetMapping("right")
    public int right() {

        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {

            slow();
            //只对List加锁
            synchronized (data) {
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
        //took:1427  响应时间
        //3000 响应结果  （结果是累加的）
    }
}
