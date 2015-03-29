package com.nyankosama.rpc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class KryoDecoder extends LengthFieldBasedFrameDecoder {
    private static Logger logger = LogManager.getLogger(KryoDecoder.class);
    private final ThreadLocal<Kryo> kryoLocalPool = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {
            return new Kryo();
        }
    };

    public KryoDecoder() {
        super(1048576, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        Kryo kryo = kryoLocalPool.get();
        try (Input input = new Input(new ByteBufInputStream(frame))) {
            return kryo.readClassAndObject(input);
        }
    }
}
