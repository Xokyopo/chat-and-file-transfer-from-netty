package org.xokyopo.clientservercommon.seirialization.executors;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.impl.Callback;
import org.xokyopo.clientservercommon.seirialization.executors.impl.FilePartStatisticMethod;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;
import org.xokyopo.clientservercommon.seirialization.executors.messages.FilePartMessage;
import org.xokyopo.clientservercommon.seirialization.executors.messages.entitys.FilePart;
import org.xokyopo.clientservercommon.seirialization.executors.utils.FilePartUtil;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessageExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Collectors;

public class FilePartExecutor extends AbstractMessageExecutor<FilePartMessage> {
    private final static String LOADING_FILE_POSTFIX = ".loading";
    private final IChannelRootDir channelRootDir;
    private final FilePartUtil filePartUtil;
    private final FilePartStatisticMethod inputStats;
    private final FilePartStatisticMethod outputStats;
    private final Callback<Channel> uploadComplete;

    public FilePartExecutor(IChannelRootDir channelRootDir, Callback<Channel> uploadComplete) {
        this(channelRootDir, uploadComplete, null, null);
    }

    public FilePartExecutor(IChannelRootDir channelRootDir, Callback<Channel> uploadComplete, FilePartStatisticMethod inputStats, FilePartStatisticMethod outputStats) {
        this.inputStats = inputStats;
        this.outputStats = outputStats;
        this.channelRootDir = (channelRootDir == null) ? (Ch) -> "." : channelRootDir;
        this.uploadComplete = uploadComplete;
        this.filePartUtil = new FilePartUtil(1024 * 1024); // partSize (byte)
    }

    @Override
    public final Class<? extends AbstractMessage> getInputMessageClass() {
        return FilePartMessage.class;
    }

    @Override
    public final boolean isLongTimeOperation() {
        return false;
    }

    @Override
    protected final void executeResponse(FilePartMessage message, Channel channel) {
        try {
            Path path = Paths.get(this.channelRootDir.getRootDir(channel), message.getDestPath(), message.getFileName());
            Path tmpPath = Paths.get(path.toString() + LOADING_FILE_POSTFIX);

            if (message.isDir()) {
                Files.createDirectories(path);
            } else {
                long currentFileLength = this.filePartUtil.saveFilePart(
                        tmpPath,
                        message.getFilePart().getPartSeed(),
                        message.getFilePart().getByteArrPart()
                );
                this.getInputStats().take(message.getFileName(), message.getFilePart().getFileLength(), currentFileLength);
                if (currentFileLength < message.getFilePart().getFileLength()) {
                    this.requestingNextPart(message.getFileName(), message.getTargetPath(), message.getDestPath(), currentFileLength, channel);
                    return;
                } else {
                    Files.deleteIfExists(path);
                    Files.move(tmpPath, path);
                }
            }

            Files.setLastModifiedTime(path, FileTime.fromMillis(message.getLastModified()));
            this.getUploadComplete().callback(channel);
        } catch (IOException e) {
            System.out.println("FilePartExecutor.filePartUtil.saveFilePart");
            e.printStackTrace();
        }
    }


    @Override
    protected final void executeRequest(FilePartMessage message, Channel channel) {
        try {
            Path workDir = Paths.get(this.channelRootDir.getRootDir(channel), message.getTargetPath());
            Path path = Paths.get(workDir.toString(), message.getFileName());

            if (path.toFile().exists()) {
                List<Path> pathList = Files.walk(path).map(e -> e.subpath(workDir.getNameCount(), e.getNameCount())).collect(Collectors.toList());
                for (Path pathToFile : pathList) {
                    this.sendFilePart(pathToFile.toString(), message.getTargetPath(), message.getDestPath(), message.getFilePart().getPartSeed(), channel);
                }
            }
        } catch (IOException e) {
            System.out.println("FilePartExecutor.filePartUtil.getFilePart");
            e.printStackTrace();
        }
    }

    @Override
    protected final void executeException(FilePartMessage message, Channel channel) {

    }

    private void requestingNextPart(String fileName, String targetPath, String destPath, long seed, Channel channel) {
        channel.writeAndFlush(
                new FilePartMessage(
                        AbstractMessage.Type.REQUEST,
                        fileName,
                        targetPath,
                        destPath,
                        false,
                        0L,
                        new FilePart(0L, seed, null)
                )
        );
    }

    private void sendFilePart(String fileName, String targetPath, String destPath, long seed, Channel channel) throws IOException {
        Path path = Paths.get(this.channelRootDir.getRootDir(channel), targetPath, fileName);
        boolean isDir = path.toFile().isDirectory();
        FilePartMessage fpm = new FilePartMessage(
                AbstractMessage.Type.RESPONSE,
                fileName,
                targetPath,
                destPath,
                isDir,
                path.toFile().lastModified(),
                (!isDir) ? new FilePart(path.toFile().length(), seed, this.filePartUtil.getFilePart(path, seed)) : null
        );
        channel.writeAndFlush(fpm);
        this.createOutputStatistic(fpm);
    }

    private void createOutputStatistic(FilePartMessage filePartMessage) {
        if (!filePartMessage.isDir() && filePartMessage.getFilePart().getFileLength() > 0) {
            this.getOutputStats().take(
                    filePartMessage.getFileName(),
                    filePartMessage.getFilePart().getFileLength(),
                    (filePartMessage.getFilePart().getPartSeed() + filePartMessage.getFilePart().getByteArrPart().length));
        }
    }

    private FilePartStatisticMethod getOutputStats() {
        return (this.outputStats == null) ? (s, fl, cl) -> {
        } : this.outputStats;
    }

    private FilePartStatisticMethod getInputStats() {
        return (this.inputStats == null) ? (s, fl, cl) -> {
        } : this.inputStats;
    }

    private Callback<Channel> getUploadComplete() {
        return (this.uploadComplete == null) ? (ch) -> {
        } : this.uploadComplete;
    }

    public void loadFile(String fileName, String targetPath, String destPath, Channel channel) {
        this.requestingNextPart(fileName, targetPath, destPath, 0, channel);
    }

    public void uploadFile(String fileName, String targetPath, String destPath, Channel channel) throws IOException {
        Path path = Paths.get(this.channelRootDir.getRootDir(channel), targetPath, fileName);
        if (path.toFile().exists()) {
            List<Path> pathList = Files.walk(path).map(e -> e.subpath(path.getNameCount() - 1, e.getNameCount())).collect(Collectors.toList());
            for (Path pathToFiles : pathList) {
                this.sendFilePart(pathToFiles.toString(), targetPath, destPath, 0, channel);
            }
        }
    }
}
