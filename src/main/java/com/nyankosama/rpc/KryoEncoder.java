package com.nyankosama.rpc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class KryoEncoder extends MessageToByteEncoder<Object>{
    private static Logger logger = LogManager.getLogger(KryoEncoder.class);
    private final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final ThreadLocal<Kryo> kryoLocalPool = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {
            return new Kryo();
        }
    };

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Kryo kryo = kryoLocalPool.get();
        int startIdx = out.writerIndex();

        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        bout.write(LENGTH_PLACEHOLDER);
        Output output = new Output(bout);
        kryo.writeClassAndObject(output, msg);
        output.close();

        int endIdx = out.writerIndex();
        out.setInt(startIdx, endIdx - startIdx - 4);
    }
}
