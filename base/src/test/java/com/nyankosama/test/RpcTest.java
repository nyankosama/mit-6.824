package com.nyankosama.test;

import com.nyankosama.base.ResultSet;
import com.nyankosama.rpc.RpcClient;
import com.nyankosama.rpc.RpcServer;
import org.junit.Test;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class RpcTest {

    public static interface Hello {
        public ResultSet<String, String> sayHi(String str);
    }

    public static class HelloImpl implements Hello {

        @Override
        public ResultSet<String, String> sayHi(String str) {
            return new ResultSet<>("Hi " + str);
        }
    }

    public static interface Echo {
        public ResultSet<String, String> echo(String str);
    }

    public static class EchoImpl implements Echo {

        @Override
        public ResultSet<String, String> echo(String str) {
            return new ResultSet<>(str);
        }
    }

    @Test
    public void benchmark() {
        RpcServer server = new RpcServer("127.0.0.1", 9123);
        server.regiester(new HelloImpl());
        new Thread(){
            @Override
            public void run() {
                server.start();
            }
        }.start();
        RpcClient client = new RpcClient("127.0.0.1", 9123);
        Hello hello = client.refer(Hello.class);

        int total = 10000;
        long begin = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            hello.sayHi("hlr");
        }
        long end = System.currentTimeMillis();
        System.out.printf("cost: %dms, qps: %f \n", (end - begin), (double)total / (end - begin) * 1000);
    }

    @Test
    public void testMultiServices() {
        RpcServer server = new RpcServer("localhost", 9123);
        server.regiester(new HelloImpl());
        server.regiester(new EchoImpl());
        new Thread(){
            @Override
            public void run() {
                server.start();
            }
        }.start();
        RpcClient client = new RpcClient("localhost", 9123);
        Hello hello = client.refer(Hello.class);
        Echo echo = client.refer(Echo.class);
        System.out.println(hello.sayHi("hlr").get());
        System.out.println(echo.echo("echo").get());
    }

}
