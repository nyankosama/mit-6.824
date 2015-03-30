package com.nyankosama.test.dubbo;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public class DubboSpringServer {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"dubbo/consumer.xml"});
        context.start();
        System.in.read(); // 按任意键退出
    }
}
