package org.xokyopo.clientservercommon.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.network.impl.IHandlerFactory;
import org.xokyopo.clientservercommon.protocol.executors.impl.IByteBufExecutor;
import org.xokyopo.clientservercommon.protocol.handlers.ByteBufHandler;
import org.xokyopo.clientservercommon.protocol.handlers.FrameEncoderDecoder;

import java.util.Objects;

public class MyProtocolHandlerFactory implements IHandlerFactory {
    private final IByteBufExecutor[] iByteBufExecutors;
    private Callback<Channel> ifConnect;
    private Callback<Channel> ifDisconnect;

    public MyProtocolHandlerFactory(Callback<Channel> ifConnect, Callback<Channel> ifDisconnect, IByteBufExecutor... iByteBufExecutors) {
        Objects.requireNonNull(iByteBufExecutors, "не задано ни одного обработчика");

        this.setIfConnect(ifConnect);
        this.setIfDisconnect(ifDisconnect);
        this.iByteBufExecutors = iByteBufExecutors;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[]{
                new FrameEncoderDecoder(),
                new ByteBufHandler(this.ifConnect, this.ifDisconnect, this.iByteBufExecutors)
        };
    }

    public void setIfConnect(Callback<Channel> ifConnect) {
        this.ifConnect = (ifConnect == null) ? ch -> {
        } : ifConnect;
    }

    public void setIfDisconnect(Callback<Channel> ifDisconnect) {
        this.ifDisconnect = (ifDisconnect == null) ? ch -> {
        } : ifDisconnect;
    }
}
