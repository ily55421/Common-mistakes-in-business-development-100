package com.ily55421.concurrency;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: linK
 * @Date: 2022/7/1 14:45
 * @Description TODO  使用了并发工具类库，线程安全就高枕无忧了吗？
 * TODO 错误一：  线程重用导致用户信息错乱的 Bug
 *
 */
@RestController
public class ThreadController {
    private ThreadLocal currentUser = ThreadLocal.withInitial(() -> null);

    /**
     * 测试公用ThreadLocal 中的数据导致读取用户信息错误
     * @param userId
     * @return
     */
    @GetMapping("wrong")
    public Map wrong(@RequestParam("userId") Integer userId) {

        //设置用户信息之前先查询一次ThreadLocal中的用户信息

        String before = Thread.currentThread().getName() + ":" + currentUser.get();

        //设置用户信息到ThreadLocal

        currentUser.set(userId);
        //设置用户信息之后再查询一次ThreadLocal中的用户信息

        String after = Thread.currentThread().getName() + ":" + currentUser.get();

        //汇总输出两次查询结果
        Map result = new HashMap();

        result.put("before", before);

        result.put("after", after);

        return result;
        // 测试结果如下
        //{
        //  "before": "http-nio-8080-exec-2:null",   设置用户之前
        //  "after": "http-nio-8080-exec-2:0"        设置用户之后
        //}

    }

    @GetMapping("right")
    public Map right(@RequestParam("userId") Integer userId) {

        String before = Thread.currentThread().getName() + ":" + currentUser.get();

        currentUser.set(userId);

        try {

            String after = Thread.currentThread().getName() + ":" + currentUser.get();
            Map result = new HashMap();
            result.put("before", before);
            result.put("after", after);
            return result;
        } finally {

            //在finally代码块中删除ThreadLocal中的数据，确保数据不串
            currentUser.remove();
        }
        // 测试结果如下
        //{
        //  "before": "http-nio-8080-exec-8:null",
        //  "after": "http-nio-8080-exec-8:2"
        //}
        // ThreadLocal 是利用独占资源的方式，来解决线程安全问题，那如果我们确实需要有资源在线程之前共享，应该怎么办呢？
        // 这时，我们可能就需要用到线程安全的容器了。
    }





}
