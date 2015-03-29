package com.nyankosama.rpc;

import com.nyankosama.base.ResultSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
@SuppressWarnings("unchecked")
public class RpcClient {
    private static Logger logger = LogManager.getLogger(RpcClient.class);
    private Map<Long, BlockingQueue<ResultSet>> threadSynMap = new ConcurrentHashMap<>();
    private Channel channel;

    public RpcClient(String ip, int port) {
        //TODO 参数错误处理
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new KryoEncoder(),
                        new KryoDecoder(),
                        new RpcClientHandler()
                );
            }
        });

        try {
            channel = b.connect(ip, port).sync().channel();
            logger.trace("client bind at:" + ip + ":" + port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public <T> T refer(final Class<T> interfaceClass) {
        //TODO 参数错误处理
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcInvocationHandler(interfaceClass.getName(), channel));
    }

    private class RpcClientHandler extends ChannelHandlerAdapter {
        private Logger logger = LogManager.getLogger(RpcInvocationHandler.class);

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof ResultObject)) {
                logger.error("handler cannot handle msg whose type is not ResultObject");
                return;
            }
            logger.trace("client handler received msg:" + msg);
            ResultObject resultObject = (ResultObject) msg;
            threadSynMap.get(resultObject.threadId).put(resultObject.resultSet);
            logger.trace("client handler put result to map");
        }
    }

    private class RpcInvocationHandler implements InvocationHandler {
        private Channel channel;
        private String interfaceName;

        public RpcInvocationHandler(String interfaceName, Channel channel) {
            this.interfaceName = interfaceName;
            this.channel = channel;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            logger.trace("invocation handler invoked");
            long threadId = Thread.currentThread().getId();
            threadSynMap.putIfAbsent(threadId, new ArrayBlockingQueue<>(1));
            CallObject callObject = new CallObject(
                    threadId,
                    interfaceName,
                    method.getName(),
                    method.getParameterTypes(),
                    args);
            channel.writeAndFlush(callObject);
            logger.trace("client proxy write and flush:" + callObject);
            return threadSynMap.get(threadId).take();
        }
    }
}
