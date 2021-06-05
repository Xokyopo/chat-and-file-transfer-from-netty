package org.xokyopo.clientservercommon.protocol.executors.impl;


import io.netty.channel.Channel;

public interface IncomingCallback<T> {
    void call(T msg, Channel channel);
}
