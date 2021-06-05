package org.xokyopo.clientservercommon.network.impl;

import io.netty.channel.ChannelHandler;

public interface IHandlerFactory {
    ChannelHandler[] getHandlers();
}
