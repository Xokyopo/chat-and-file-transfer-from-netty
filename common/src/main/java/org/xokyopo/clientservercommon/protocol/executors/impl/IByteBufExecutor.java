package org.xokyopo.clientservercommon.protocol.executors.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public interface IByteBufExecutor {
    byte getSignalByte();

    void executeMessage(Channel channel, ByteBuf byteBuf);
}
