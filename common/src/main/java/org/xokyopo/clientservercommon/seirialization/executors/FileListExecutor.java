package org.xokyopo.clientservercommon.seirialization.executors;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;
import org.xokyopo.clientservercommon.seirialization.executors.messages.FileListMessage;
import org.xokyopo.clientservercommon.seirialization.executors.messages.entitys.FileRep;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessageExecutor;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileListExecutor extends AbstractMessageExecutor<FileListMessage> {
    private final IChannelRootDir channelRootDir;
    private final Callback<List<FileRep>> fileListResponse;

    public FileListExecutor(IChannelRootDir channelRootDir, Callback<List<FileRep>> fileListResponse) {
        this.channelRootDir = (channelRootDir == null) ? (Ch) -> "." : channelRootDir;
        this.fileListResponse = fileListResponse;
    }

    @Override
    public Class<? extends AbstractMessage> getInputMessageClass() {
        return FileListMessage.class;
    }

    @Override
    public boolean isLongTimeOperation() {
        return false;
    }

    @Override
    protected void executeResponse(FileListMessage message, Channel channel) {
        this.getFileListResponse().callback(message.getFileList());
    }

    @Override
    protected void executeRequest(FileListMessage message, Channel channel) {
        File path = Paths.get(channelRootDir.getRootDir(channel), message.getPath()).toFile();
        File[] FileArr = path.listFiles();
        if (FileArr != null) {
            channel.writeAndFlush(new FileListMessage(
                    AbstractMessage.Type.RESPONSE,
                    Arrays.stream(FileArr).map(FileRep::new).collect(Collectors.toList()),
                    null)
            );
        }
    }

    @Override
    protected void executeException(FileListMessage message, Channel channel) {

    }

    public void getFileList(String path, Channel channel) {
        channel.writeAndFlush(new FileListMessage(AbstractMessage.Type.REQUEST, null, path));
    }

    private Callback<List<FileRep>> getFileListResponse() {
        return (this.fileListResponse == null) ? a -> {
        } : this.fileListResponse;
    }
}
