package org.xokyopo.clientservercommon.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.xokyopo.clientservercommon.network.impl.IHandlerFactory;

public class NettyClientConnection {
    private final ChannelInitializer<? extends Channel> channelInitializer;

    public NettyClientConnection(IHandlerFactory handlerFactory) {
        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast(handlerFactory.getHandlers());
            }
        };
    }

    public void run(String host, int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();

        try {
            ChannelFuture future = this.getBootstrap(bossGroup, this.channelInitializer).connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

    private Bootstrap getBootstrap(EventLoopGroup bossGroup, ChannelInitializer<? extends Channel> channelInitializer) {
        return (new Bootstrap())
                .group(bossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(channelInitializer);
    }
}
