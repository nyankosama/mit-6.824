package com.nyankosama.test.dubbo;

import com.alibaba.dubbo.config.*;

import java.io.IOException;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public class DubboServerTest {

    public static void main(String[] args) throws IOException {

        // 服务实现
        Hello hello = new HelloIpml();

        // 当前应用配置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("helloServer");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");

        // 服务提供者协议配置
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(12345);
        protocol.setThreads(200);

        // 注意：ServiceConfig为重对象，内部封装了与注册中心的连接，以及开启服务端口

        // 服务提供者暴露服务配置
        ServiceConfig<Hello> service = new ServiceConfig<>(); // 此实例很重，封装了与注册中心的连接，请自行缓存，否则可能造成内存和连接泄漏
        service.setApplication(application);
        service.setRegistry(registry); // 多个注册中心可以用setRegistries()
        service.setProtocol(protocol); // 多个协议可以用setProtocols()
        service.setInterface(Hello.class);
        service.setRef(hello);
        service.setVersion("1.0.0");

        // 暴露及注册服务
        service.export();
        System.in.read();
    }

}
