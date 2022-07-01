package com.ily55421.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 15:36
 * @Description TODO  错误四： 没有认清并发工具的使用场景，因而导致性能问题
 * 比较下使用 CopyOnWriteArrayList 和普通加锁方式 ArrayList 的读写性能吧。
 */
@RestController
@RequestMapping("concurrencyWrite")
@Slf4j
public class ConcurrencyWriteController {
    /**
     * 测试CopyOnWriteArrayList 和 ArrayList的写性能
     * @return
     */
    @GetMapping("write")
    public Map testWrite() {
         // CopyOnWriteArrayList
         List copyOnWriteArrayList = new CopyOnWriteArrayList<>();
         List synchronizedList = Collections.synchronizedList(new ArrayList<>());
         StopWatch stopWatch = new StopWatch();

         int loopCount = 100000;
         stopWatch.start("Write:copyOnWriteArrayList");

         //循环100000次并发往CopyOnWriteArrayList写入随机元素
         IntStream.
        rangeClosed(1, loopCount).parallel().forEach(A -> copyOnWriteArrayList.add(ThreadLocalRandom.current().nextInt(loopCount)));
         stopWatch.stop();
         stopWatch.start("Write:synchronizedList");

         //循环100000次并发往加锁的ArrayList写入随机元素
         IntStream.
        rangeClosed(1, loopCount).parallel().forEach(A -> synchronizedList.add(ThreadLocalRandom.current().nextInt(loopCount)));
         stopWatch.stop();
         log.info(stopWatch.prettyPrint());
         Map result = new HashMap();
         result.put("copyOnWriteArrayList", copyOnWriteArrayList.size());
         result.put("synchronizedList", synchronizedList.size());
         return result;
         // 结果如下： synchronizedList的运行性能差距太大了吧
        // 运行程序可以看到，大量写的场景（10 万次 add 操作），CopyOnWriteArray 几乎比同步的 ArrayList 慢一百倍：
         // ns         %     Task name
        //---------------------------------------------
        //3600515600  099%  Write:copyOnWriteArrayList
        //034712000  001%  Write:synchronizedList


        // 1000万次 add操作 CopyOnWriteArray 几乎比同步的 ArrayList 快一百倍：
        //ns         %     Task name
        //---------------------------------------------
        //3409060200  099%  Write:copyOnWriteArrayList
        //034167400  001%  Write:synchronizedList
    }

    /**
     * 帮助方法用来填充List
     * @param list
     */
    private void addAll(List list) {
        
         // boxed 装盒
         list.addAll(IntStream.rangeClosed(1, 1000000).boxed().collect(Collectors.toList()));
//         list.addAll(IntStream.rangeClosed(1, 10000000).boxed().collect(Collectors.toList()));
//         list.addAll(IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList()));
    }

    /**
     * 测试并发读的性能
     * @return
     */
    @GetMapping("read")
    public Map testRead() {

         //创建两个测试对象
         List copyOnWriteArrayList = new CopyOnWriteArrayList<>();
         List synchronizedList = Collections.synchronizedList(new ArrayList<>());

         //填充数据
         addAll (copyOnWriteArrayList);
         addAll (synchronizedList);
         StopWatch stopWatch = new StopWatch();
         int loopCount = 1000000;
         int count = copyOnWriteArrayList.size();
         stopWatch.start("Read:copyOnWriteArrayList");

         //循环1000000次并发从CopyOnWriteArrayList随机查询元素
         IntStream.
        rangeClosed(1, loopCount).parallel().forEach(A -> copyOnWriteArrayList.get(ThreadLocalRandom.current().nextInt(count)));

         stopWatch.stop();
         stopWatch.start("Read:synchronizedList");

         //循环1000000次并发从加锁的ArrayList随机查询元素
         IntStream.
         range(0, loopCount).parallel().forEach(A -> synchronizedList.get(ThreadLocalRandom.current().nextInt(count)));
         stopWatch.stop();
         log.info(stopWatch.prettyPrint());

         Map result = new HashMap();
         result.put("copyOnWriteArrayList", copyOnWriteArrayList.size());
         result.put("synchronizedList", synchronizedList.size());
         return result;
         // 测试结果: 读取的值都是正确的 没有异常
         // {
        //  "copyOnWriteArrayList": 1000000,
        //  "synchronizedList": 1000000
        //}
    }
}
