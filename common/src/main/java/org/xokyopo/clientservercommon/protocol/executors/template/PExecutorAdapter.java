package org.xokyopo.clientservercommon.protocol.executors.template;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.protocol.executors.impl.IByteBufExecutor;

public abstract class PExecutorAdapter implements IByteBufExecutor {
    @Override
    public abstract byte getSignalByte();

    @Override
    public final void executeMessage(Channel channel, ByteBuf byteBuf) {
        byte signal = byteBuf.readByte();
        if (signal == MessageType.REQUEST.signal) {
            try {
                this.executeRequest(channel, byteBuf);
            } catch (Exception e) {
                System.out.println(this.getClass().getCanonicalName() + "executeMessage exception");
                e.printStackTrace();
            }
        } else if (signal == MessageType.RESPONSE.signal) {
            this.executeResponse(channel, byteBuf);
        } else if (signal == MessageType.FINISH.signal) {
            this.executeFinish(channel, byteBuf);
        } else {
            this.executeError(channel, byteBuf);
        }
    }

    public void executeRequest(Channel channel, ByteBuf byteBuf) {
    }

    public void executeFinish(Channel channel, ByteBuf byteBuf) {
    }

    ;

    public void executeResponse(Channel channel, ByteBuf byteBuf) {
    }

    ;

    public void executeError(Channel channel, ByteBuf byteBuf) {
    }

    ;

    protected final void sendShortSignal(byte signal, Channel channel) {
        ByteBuf outBuff = channel.alloc().buffer(2);
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(signal);
        channel.writeAndFlush(outBuff);
    }

    ;

    protected void sendFinish(Channel channel) {
        this.sendShortSignal(MessageType.FINISH.signal, channel);
    }

    protected void sendError(Channel channel) {
        this.sendShortSignal(MessageType.ERROR.signal, channel);
    }

    protected enum MessageType {
        REQUEST((byte) 1),
        RESPONSE((byte) 2),
        FINISH((byte) 3),
        ERROR((byte) 4);
        public final byte signal;

        MessageType(byte signal) {
            this.signal = signal;
        }
    }
}
