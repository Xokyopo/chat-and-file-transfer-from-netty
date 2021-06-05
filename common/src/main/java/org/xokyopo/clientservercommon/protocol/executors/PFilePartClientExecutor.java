package org.xokyopo.clientservercommon.protocol.executors;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.executors.entitys.PFileInfo;
import org.xokyopo.clientservercommon.protocol.executors.entitys.QueueElement;
import org.xokyopo.clientservercommon.protocol.executors.impl.FileTransferStatistic;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PFilePartClientExecutor extends PFilePartServerExecutor {
    private final Set<QueueElement> uploadQueue;
    private final Set<QueueElement> loadQueue;
    private Callback<Boolean> transferCompleteCallback;
    private QueueElement currentLoad;

    private QueueElement currentUpload;

    public PFilePartClientExecutor(IChannelRootDir channelRootDir) {
        this(channelRootDir, null);
    }

    public PFilePartClientExecutor(IChannelRootDir channelRootDir, Callback<Boolean> transferCompleteCallback) {
        this(channelRootDir, transferCompleteCallback, null, null);
    }

    public PFilePartClientExecutor(IChannelRootDir channelRootDir, Callback<Boolean> transferCompleteCallback, FileTransferStatistic inputStats, FileTransferStatistic outputStats) {
        super(channelRootDir, inputStats, outputStats);
        this.setTransferCompleteCallback(transferCompleteCallback);
        this.uploadQueue = Collections.synchronizedSet(new HashSet<>());
        this.loadQueue = Collections.synchronizedSet(new HashSet<>());
    }

    public void setTransferCompleteCallback(Callback<Boolean> transferCompleteCallback) {
        this.transferCompleteCallback = (transferCompleteCallback == null) ? (bool) -> {
        } : transferCompleteCallback;
    }

    @Override
    public void executeFinish(Channel channel, ByteBuf byteBuf) {
        this.uploadFileComplete(channel);
    }

    @Override
    public void executeError(Channel channel, ByteBuf byteBuf) {
        System.out.println("Ошибка executeError");
    }

    @Override
    protected void sendFinish(Channel channel) {
        this.loadFileComplete(channel);
    }

    protected void loadFileComplete(Channel channel) {
        this.transferCompleteCallback.callback(true);
        this.currentLoad = null;
        this.loadNextFile(channel);
    }

    protected void uploadFileComplete(Channel channel) {
        this.transferCompleteCallback.callback(true);
        this.currentUpload = null;
        this.uploadNextFile(channel);
    }

    private void loadNextFile(Channel channel) {
        if (this.loadQueue.size() > 0 && this.currentLoad == null) {
            this.currentLoad = this.loadQueue.stream().findAny().get();
            this.loadQueue.remove(this.currentLoad);
            this.getFile(this.currentLoad.sources, channel);
        }
    }

    private void uploadNextFile(Channel channel) {
        if (this.uploadQueue.size() > 0 && this.currentUpload == null) {
            this.currentUpload = this.uploadQueue.stream().findAny().get();
            this.uploadQueue.remove(currentUpload);
            Path filePath = Paths.get(this.currentUpload.sources);
            try {
                if (filePath.toFile().isDirectory()) {
                    Files.list(filePath).map(path -> path.getFileName().toString()).forEach(fileName -> {
                        this.addInUploadQueue(
                                Paths.get(this.currentUpload.sources, fileName).toString(),
                                Paths.get(this.currentUpload.dest, fileName).toString()
                        );
                    });
                }
                this.sendFile(Paths.get(this.currentUpload.sources), channel);
            } catch (IOException e) {
                //TODO ошибки обработка чтения файла.
                System.out.println("PFilePartClientExecutor.uploadNextFile ошибка доступа к файловой системе для " + filePath);
                e.printStackTrace();
            }
        }
    }

    private void addInLoadQueue(String source, String dest) {
        this.loadQueue.add(new QueueElement(source, dest));
    }

    private void addInUploadQueue(String source, String dest) {
        this.uploadQueue.add(new QueueElement(source, dest));
    }

    @Override
    protected Path getLoadToPath(String fileName, Channel channel) {
        return Paths.get(this.currentLoad.dest);
    }

    @Override
    protected Path getUploadFromPath(String fileName, Channel channel) {
        return Paths.get(this.currentUpload.sources);
    }

    @Override
    protected String getUploadOutcomePath(Path fileName, Channel channel) {
        return this.currentUpload.dest;
    }

    @Override
    protected void saveDir(PFileInfo pFileInfo, ByteBuf dirData, Channel channel) throws IOException {
        List<String> fileNameList = MyByteBufUtil.getListOfString(dirData);
        fileNameList.forEach(fileName -> {
            this.addInLoadQueue(
                    Paths.get(this.currentLoad.sources, fileName).toString(),
                    Paths.get(this.currentLoad.dest, fileName).toString()
            );
        });
        super.saveDir(pFileInfo, dirData, channel);
    }

    @Override
    //Переопределено, для того что бы убрать список файлы из того что отправляется на сервер.
    protected void sendDir(Path fileFrom, Channel channel) throws IOException {
        ByteBuf outBuf = channel.alloc().buffer(3 + PFileInfo.countLengthInByte(fileFrom.toString()));
        ByteBuf fileInfoBuffer = PFileInfo.getByte(fileFrom.toFile(), this.getUploadOutcomePath(fileFrom, channel));
        try {
            outBuf.writeByte(this.getSignalByte());
            outBuf.writeByte(MessageType.RESPONSE.signal);
            outBuf.writeByte(Type.DIR.signal);

            outBuf.writeBytes(fileInfoBuffer);
            channel.writeAndFlush(outBuf);
        } finally {
            ReferenceCountUtil.release(fileInfoBuffer);
        }
    }

    public void sendFile(String fileName, String sourceDir, String destDir, Channel channel) {
        this.addInUploadQueue(
                Paths.get(this.channelRootDir.getRootDir(channel), sourceDir, fileName).toString(),
                Paths.get(destDir, fileName).toString()
        );
        this.uploadNextFile(channel);
    }

    public void getFile(String fileName, String sourceDir, String destDir, Channel channel) {
        this.addInLoadQueue(
                Paths.get(sourceDir, fileName).toString(),
                Paths.get(this.channelRootDir.getRootDir(channel), destDir, fileName).toString()
        );
        this.loadNextFile(channel);
    }
}
