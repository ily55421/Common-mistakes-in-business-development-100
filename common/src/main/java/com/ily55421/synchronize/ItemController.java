package com.ily55421.synchronize;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: linK
 * @Date: 2022/7/1 16:48
 * @Description TODO   多把锁要小心死锁问题
 */
@Slf4j
@RestController
@RequestMapping("item")
public class ItemController {
    static List<Item> itemList = Arrays.asList(new Item("item1"),
            new Item("item2"), new Item("item3"),
            new Item("item4"), new Item("item5"),
            new Item("item6"), new Item("item7"),
            new Item("item8"), new Item("item9"),
            new Item("item10"));
    static Map<String, List<Item>> items = new HashMap<>();

    static {
        items.put("1", itemList);
        items.put("2", itemList);
        items.put("3", itemList);
    }

    @GetMapping("wrong")
    public long wrong() {
        long begin = System.currentTimeMillis();
        Map<Integer, List<Item>> items = new HashMap<>();

        //并发进行100次下单操作，统计成功次数
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List cart = createCart();
                    return createOrder(cart);
                })
//                .filter(result -> result)
                .count();
        log.info("success:{} totalRemaining:{} took:{}ms items:{}",
                success,
                items.entrySet().stream().map(item -> items.get(item).get(0).remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin, items);

        return success;
        // success:100 totalRemaining:0 took:9ms items:{}
        // 100 返回结果
    }

    @GetMapping("right")

    public long right() {
        long success = 100;
//        long success = IntStream.rangeClosed(1, 100).parallel()
//                .mapToObj(i -> {
//                    List cart = createCart().stream()
//                            .sorted(Comparator.comparing(Item::getName))
//                            .collect(Collectors.toList());
//                    return createOrder(cart);
//                })
//                .filter(result -> result)
//                .count();
        return success;
    }

    private boolean createOrder(List<Item> order) {

        //存放所有获得的锁
        List<ReentrantLock> locks = new ArrayList<>();
        for (Item item : order) {

            try {
                //获得锁10秒超时
                if (item.lock.tryLock(10, TimeUnit.SECONDS)) {
                    locks.add(item.lock);
                } else {
                    locks.forEach(ReentrantLock::unlock);
                    return false;
                }
            } catch (InterruptedException e) {
            }
        }

        //锁全部拿到之后执行扣减库存业务逻辑
        try {
            order.forEach(item -> item.remaining--);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
        return true;
    }


    /**
     * 模拟在购物车进行商品选购，每次从商品清单（items 字段）中随机选购三个商品
     * （为了逻辑简单，我们不考虑每次选购多个同类商品的逻辑，购物车中不体现商品数量）
     *
     * @return
     */
    public List createCart() {
        return items.get(ThreadLocalRandom.current().nextInt(items.size()) + 1);
    }


    @Data
    @RequiredArgsConstructor
    static class Item {

        final String name; //商品名

        int remaining = 1000; //库存剩余

        @ToString.Exclude //ToString不包含这个字段
        ReentrantLock lock = new ReentrantLock();
    }

}
