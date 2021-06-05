package org.xokyopo.clientservercommon.protocol.executors;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.executors.entitys.PFileInfo;
import org.xokyopo.clientservercommon.protocol.executors.template.ExecutorSignalByte;
import org.xokyopo.clientservercommon.protocol.executors.template.PExecutorAdapter;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PFileListExecutor extends PExecutorAdapter {
    private IChannelRootDir channelRootDir;
    private Callback<List<PFileInfo>> fileListResponse;

    public PFileListExecutor(IChannelRootDir channelRootDir, Callback<List<PFileInfo>> fileListResponse) {
        this.setChannelRootDir(channelRootDir);
        this.setFileListResponse(fileListResponse);
    }

    @Override
    public byte getSignalByte() {
        return ExecutorSignalByte.FILE_LIST.getSignal();
    }

    @Override
    public void executeRequest(Channel channel, ByteBuf byteBuf) {
        Path path = Paths.get(channelRootDir.getRootDir(channel), MyByteBufUtil.getString(byteBuf));
        this.sendResponse(this.getEncodeFileList(path), channel);
    }

    @Override
    public void executeResponse(Channel channel, ByteBuf byteBuf) {
        this.fileListResponse.callback(this.getDecodeFileList(byteBuf));
    }


    private void sendResponse(List<ByteBuf> fileList, Channel channel) {
        ByteBuf outBuff = channel.alloc().buffer(2 + fileList.stream().mapToInt(ByteBuf::readableBytes).sum());
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(MessageType.RESPONSE.signal);
        MyByteBufUtil.writeByteBufList(fileList, outBuff);
        channel.writeAndFlush(outBuff);
    }

    public void sendRequest(String path, Channel channel) {
        ByteBuf outBuff = channel.alloc().buffer(2 + MyByteBufUtil.getTextLength(path));
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(MessageType.REQUEST.signal);
        MyByteBufUtil.addString(path, outBuff);
        channel.writeAndFlush(outBuff);
    }

    public void setChannelRootDir(IChannelRootDir channelRootDir) {
        this.channelRootDir = (channelRootDir == null) ? ch -> "." : channelRootDir;
    }

    public void setFileListResponse(Callback<List<PFileInfo>> fileListResponse) {
        this.fileListResponse = (fileListResponse == null) ? list -> {
        } : fileListResponse;
    }

    private List<ByteBuf> getEncodeFileList(Path path) {
        File[] fileArr = path.toFile().listFiles();
        if (fileArr == null) return new ArrayList<>();
        else return Arrays.stream(fileArr).map(PFileInfo::getByte).collect(Collectors.toList());
    }

    private List<PFileInfo> getDecodeFileList(ByteBuf byteBuf) {
        List<PFileInfo> fileList = new ArrayList<>();
        while (byteBuf.readableBytes() > 0) fileList.add(new PFileInfo(byteBuf));
        return fileList;
    }
}
