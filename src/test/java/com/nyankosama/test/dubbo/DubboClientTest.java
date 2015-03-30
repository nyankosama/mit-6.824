package com.nyankosama.test.dubbo;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public class DubboClientTest {

    public static void main(String[] args) {
        // 当前应用配置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("helloClient");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");

        // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接

        // 引用远程服务
        ReferenceConfig<Hello> reference = new ReferenceConfig<>(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
        reference.setApplication(application);
        reference.setRegistry(registry); // 多个注册中心可以用setRegistries()
        reference.setInterface(Hello.class);
        reference.setVersion("1.0.0");

        // 和本地bean一样使用xxxService
        Hello hello = reference.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用
        System.out.println("init succeed");
        int total = 10000;
        long begin = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            hello.sayHi("hlr");
        }
        long end = System.currentTimeMillis();
        System.out.printf("cost=%dms, qps=%f", (end - begin), (double)total / (end - begin) * 1000);
    }
}
