package com.nyankosama.rpc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ObjectDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class KryoDecoder extends ObjectDecoder{
    private static Logger logger = LogManager.getLogger(KryoDecoder.class);
    private final ThreadLocal<Kryo> kryoLocalPool = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {
            return new Kryo();
        }
    };

    private String name;

    public KryoDecoder(String name, int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, classResolver);
        this.name = name;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        logger.trace("decoder:" + name + " called");
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            logger.error("decoder:" + name + " frame is null");
            return null;
        }
        Kryo kryo = kryoLocalPool.get();
        try (Input input = new Input(new ByteBufInputStream(frame))) {
            return kryo.readClassAndObject(input);
        }
    }
}
