package org.xokyopo.clientservercommon.network.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.xokyopo.clientservercommon.network.impl.IHandlerFactory;

public class NettyServerConnection {
    private final ChannelInitializer<? extends Channel> channelInitializer;

    public NettyServerConnection(IHandlerFactory handlerFactory) {
        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast(handlerFactory.getHandlers());
            }
        };
    }

    public void run(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ChannelFuture f = this.getServerBootstrap(bossGroup, workerGroup, this.channelInitializer).bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private ServerBootstrap getServerBootstrap(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ChannelInitializer<? extends Channel> channelInitializer) {
        return (new ServerBootstrap())
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(channelInitializer);
    }
}
