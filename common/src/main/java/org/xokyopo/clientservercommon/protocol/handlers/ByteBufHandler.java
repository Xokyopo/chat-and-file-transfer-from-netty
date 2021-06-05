package org.xokyopo.clientservercommon.protocol.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.protocol.executors.impl.IByteBufExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Sharable
public class ByteBufHandler extends ChannelInboundHandlerAdapter {
    private final Callback<Channel> connectCallback;
    private final Callback<Channel> disconnectCallback;
    private final Map<Byte, IByteBufExecutor> byteIByteBufExecutorMap;

    public ByteBufHandler(Callback<Channel> connectCallback, Callback<Channel> disconnectCallback, IByteBufExecutor... iByteBufExecutors) {
        this.byteIByteBufExecutorMap = Arrays.stream(iByteBufExecutors).collect(Collectors.toMap(IByteBufExecutor::getSignalByte, value -> value));
        this.connectCallback = connectCallback;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.connectCallback.callback(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.disconnectCallback.callback(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf input = (ByteBuf) msg;
        try {
            IByteBufExecutor byteBufExecutor = this.byteIByteBufExecutorMap.get(input.readByte());
            if (byteBufExecutor != null) {
                byteBufExecutor.executeMessage(ctx.channel(), input);
            }
        } finally {
            ReferenceCountUtil.release(input);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        System.out.println("MyHandler.exceptionCaught");
        ctx.close();
    }
}
