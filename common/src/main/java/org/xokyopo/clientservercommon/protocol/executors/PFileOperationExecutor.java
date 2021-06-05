package org.xokyopo.clientservercommon.protocol.executors;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.executors.template.ExecutorSignalByte;
import org.xokyopo.clientservercommon.protocol.executors.template.PExecutorAdapter;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;
import org.xokyopo.clientservercommon.utils.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PFileOperationExecutor extends PExecutorAdapter {
    private IChannelRootDir channelRootDir;
    private Callback<Boolean> remoteOperationResponse;

    public PFileOperationExecutor(IChannelRootDir channelRootDir, Callback<Boolean> remoteOperationResponse) {
        this.setChannelRootDir(channelRootDir);
        this.setRemoteOperationResponse(remoteOperationResponse);
    }

    @Override
    public byte getSignalByte() {
        return ExecutorSignalByte.FILE_OPERATION.getSignal();
    }

    @Override
    public void executeRequest(Channel channel, ByteBuf byteBuf) {
        byte signal = byteBuf.readByte();
        try {
            if (signal == FileOperation.MOVE.signal) {
                Files.move(this.getFilePath(byteBuf, channel), this.getFilePath(byteBuf, channel));
            } else if (signal == FileOperation.DELETE.signal) {
                FileUtil.recurseDelete(this.getFilePath(byteBuf, channel));
            }
            this.sendFinish(channel);
        } catch (Exception e) {
            this.sendError(channel);
        }
    }

    @Override
    public void executeFinish(Channel channel, ByteBuf byteBuf) {
        this.remoteOperationResponse.callback(true);
    }

    @Override
    public void executeError(Channel channel, ByteBuf byteBuf) {
        this.remoteOperationResponse.callback(false);
    }

    private Path getFilePath(ByteBuf byteBuf, Channel channel) {
        return Paths.get(this.channelRootDir.getRootDir(channel), MyByteBufUtil.getString(byteBuf));
    }

    private void sendRequest(FileOperation type, String oldFileName, String newFileName, Channel channel) {
        int messageTextLength = (type.equals(FileOperation.MOVE)) ? MyByteBufUtil.getTextLength(oldFileName, newFileName) : MyByteBufUtil.getTextLength(oldFileName);
        ByteBuf outBuff = channel.alloc().buffer(3 + messageTextLength);
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(MessageType.REQUEST.signal);
        outBuff.writeByte(type.signal);
        MyByteBufUtil.addString(oldFileName, outBuff);
        if (type.equals(FileOperation.MOVE)) MyByteBufUtil.addString(newFileName, outBuff);
        channel.writeAndFlush(outBuff);
    }

    public void moveFile(String oldFileName, String newFileName, Channel channel) {
        this.sendRequest(FileOperation.MOVE, oldFileName, newFileName, channel);
    }

    public void deleteFile(String fileName, Channel channel) {
        this.sendRequest(FileOperation.DELETE, fileName, null, channel);
    }

    public void setChannelRootDir(IChannelRootDir channelRootDir) {
        this.channelRootDir = (channelRootDir == null) ? ch -> "." : channelRootDir;
    }

    public void setRemoteOperationResponse(Callback<Boolean> remoteOperationResponse) {
        this.remoteOperationResponse = (remoteOperationResponse == null) ? b -> {
        } : remoteOperationResponse;
    }

    private enum FileOperation {
        MOVE((byte) 1),
        DELETE((byte) 2),
        ;
        public final byte signal;

        FileOperation(byte signal) {
            this.signal = signal;
        }
    }
}
