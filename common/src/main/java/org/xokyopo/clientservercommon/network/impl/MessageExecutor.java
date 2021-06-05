package org.xokyopo.clientservercommon.network.impl;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

public interface MessageExecutor {
    Class<? extends AbstractMessage> getInputMessageClass();

    boolean isLongTimeOperation();

    void execute(AbstractMessage abstractMessage, Channel channel);
}
