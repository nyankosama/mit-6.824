package com.nyankosama.test.dubbo;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public class DubboSpringClient {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"dubbo/consumer.xml"});
        context.start();

        Hello demoService = (Hello)context.getBean("demoService"); // 获取远程服务代理
        String hello = demoService.sayHi("world"); // 执行远程方法

        System.out.println( hello ); // 显示调用结果
    }
}
