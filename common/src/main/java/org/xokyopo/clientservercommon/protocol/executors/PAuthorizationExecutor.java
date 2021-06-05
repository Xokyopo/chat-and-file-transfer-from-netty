package org.xokyopo.clientservercommon.protocol.executors;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.executors.template.ExecutorSignalByte;
import org.xokyopo.clientservercommon.protocol.executors.template.PExecutorAdapter;
import org.xokyopo.clientservercommon.seirialization.executors.impl.AuthorisationMethod;

public class PAuthorizationExecutor extends PExecutorAdapter {
    private Callback<Boolean> authorizationCallback;
    private AuthorisationMethod authorisationMethod;

    public PAuthorizationExecutor(Callback<Boolean> authorizationCallback, AuthorisationMethod authorisationMethod) {
        this.authorizationCallback = authorizationCallback;
        this.authorisationMethod = authorisationMethod;
    }

    @Override
    public byte getSignalByte() {
        return ExecutorSignalByte.AUTHORIZATION.getSignal();
    }

    @Override
    public void executeRequest(Channel channel, ByteBuf byteBuf) {
        if (this.authorisationMethod.check(MyByteBufUtil.getString(byteBuf), MyByteBufUtil.getString(byteBuf), channel)) {
            this.sendFinish(channel);
        } else {
            this.sendError(channel);
        }
    }

    @Override
    public void executeFinish(Channel channel, ByteBuf byteBuf) {
        this.authorizationCallback.callback(true);
    }

    @Override
    public void executeError(Channel channel, ByteBuf byteBuf) {
        this.authorizationCallback.callback(false);
    }

    public void setAuthorizationResponse(Callback<Boolean> authorizationCallback) {
        this.authorizationCallback = (authorizationCallback == null) ? bool -> {
        } : authorizationCallback;
    }

    public void setAuthorisationMethod(AuthorisationMethod authorisationMethod) {
        this.authorisationMethod = (authorisationMethod == null) ? (login, pass, ch) -> false : authorisationMethod;
    }

    public void sendLoginAndPass(String login, String pass, Channel channel) {
        ByteBuf outBuff = channel.alloc().buffer(2 + MyByteBufUtil.getTextLength(login, pass));
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(MessageType.REQUEST.signal);
        MyByteBufUtil.addString(login, outBuff);
        MyByteBufUtil.addString(pass, outBuff);
        channel.writeAndFlush(outBuff);
    }
}
