package org.geekbang.time.commonmistakes.advancedfeatures.genericandinheritance;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: linK
 * @Date: 2022/7/4 17:30
 * @Description
 * <p>不编写客户端，通过windows自带的telnet进行测试</p>
 * <p>1.命令：telnet 127.0.0.1 6666</p>
 * <p>2.Ctrl+]快捷键进入telnet命令窗口</p>
 * <p>3.通过send命令发送消息(如：send hello)</p>
 */
public class BIOServer {

    public static void main(String[] args) throws IOException {
        // 线程池机制

        // 思路
        //1.创建一个线程池

        //2.如果有客户端链接，就创建一个线程与之通信

        ExecutorService executorService = Executors.newCachedThreadPool();

        // 创建ServerSocket
        ServerSocket serverSocket = new ServerSocket(6666);
        System.out.println("服务器启动了");

        while (true) {
            // 监听，等待客户端链接
            System.out.println("等待连接");
            final Socket socket = serverSocket.accept();
            System.out.println("连接到一个客户端");

            // 创建线程进行通信
            executorService.submit(() -> {
                handler(socket);
            });

        }

    }

    // 与客户端进行通信
    public static void handler(Socket socket) {
        try {
            System.out.println("线程ID = " + Thread.currentThread().getId() + "，线程名称Name = " + Thread.currentThread().getName());
            byte[] bytes = new byte[1024];

            // 获取输入流
            InputStream is = socket.getInputStream();

            while (true) {
                System.out.println("线程ID = " + Thread.currentThread().getId() + "，线程名称Name = " + Thread.currentThread().getName());
                System.out.println("read....");
                int len = is.read(bytes);
                System.out.println(new String(bytes, 0, len));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("关闭client的连接");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
