package org.xokyopo.clientservercommon.seirialization.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.network.impl.MessageExecutor;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {
    private final Map<Class<? extends AbstractMessage>, MessageExecutor> messageExecutorMap;
    private final Callback<Channel> ifConnect;
    private final Callback<Channel> ifDisconnect;

    public MessageHandler(Callback<Channel> ifConnect, Callback<Channel> ifDisconnect, MessageExecutor... messageExecutors) {
        this.messageExecutorMap = Arrays.stream(messageExecutors).collect(Collectors.toMap(MessageExecutor::getInputMessageClass, value -> value));
        this.ifConnect = ifConnect;
        this.ifDisconnect = ifDisconnect;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AbstractMessage) {
            final AbstractMessage abstractMessage = (AbstractMessage) msg;
            MessageExecutor messageExecutor = this.messageExecutorMap.get(abstractMessage.getClass());
            if (messageExecutor != null) {
                if (messageExecutor.isLongTimeOperation()) {
                    new Thread(() -> messageExecutor.execute(abstractMessage, ctx.channel())).start();
                } else {
                    messageExecutor.execute(abstractMessage, ctx.channel());
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //ignoring error
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ifConnect.callback(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.ifDisconnect.callback(ctx.channel());
        super.channelInactive(ctx);
    }
}
