package com.nyankosama.rpc;

import com.nyankosama.base.ResultSet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class RpcServer {
    private static Logger logger = LogManager.getLogger(RpcServer.class);
    private String ip;
    private int port;
    private final Map<String, ServiceHandler> exportedServices = new ConcurrentHashMap<>();

    public RpcServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void regiester(final Object service) {
        exportedServices.put(service.getClass().getInterfaces()[0].getName(), new ServiceHandler(service));
        logger.trace("register service:" + service);
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(1048576, ClassResolvers.cacheDisabled(null)),
                                    new RpcServerHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            logger.trace("bind server at:" + ip + ":" + port);

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private class RpcServerHandler extends ChannelHandlerAdapter {
        private Logger logger = LogManager.getLogger(RpcServerHandler.class);

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            logger.trace("client connection received");
        }

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            logger.trace("server handler object received:" + msg);
            if (!(msg instanceof CallObject)) {
                logger.error("handler cannot handle msg whose type is not CallObject");
                ctx.writeAndFlush(new ResultObject(1, null));
                return;
            }
            CallObject callObject = (CallObject) msg;
            ResultSet<?, ?> resultSet;
            ServiceHandler handler = exportedServices.getOrDefault(callObject.interfaceName, null);
            logger.trace("server handler get service handler");
            if (handler == null) {
                logger.trace("cannot find exported service");
                resultSet = new ResultSet<>(null, false, "cannot find exported service!");
            } else {
                logger.trace("try invoke");
                resultSet = handler.tryInvoke(callObject.methodName, callObject.parameterTypes, callObject.arguments);
            }
            ctx.writeAndFlush(new ResultObject(callObject.threadId, resultSet));
            logger.trace("write back result:" + resultSet);
        }
    }
}
