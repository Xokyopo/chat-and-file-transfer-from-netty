package org.xokyopo.clientservercommon.protocol.executors;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.TypeLengthUtilInByte;
import org.xokyopo.clientservercommon.protocol.executors.entitys.PFileInfo;
import org.xokyopo.clientservercommon.protocol.executors.impl.FileTransferStatistic;
import org.xokyopo.clientservercommon.protocol.executors.template.ExecutorSignalByte;
import org.xokyopo.clientservercommon.protocol.executors.template.PExecutorAdapter;
import org.xokyopo.clientservercommon.seirialization.executors.impl.IChannelRootDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Collectors;


public class PFilePartServerExecutor extends PExecutorAdapter {
    protected final static int FILE_PART_BUFFER_LENGTH = 1042 * 1024; //1MB
    protected final static String LOADING_FILE_POSTFIX = ".loading";
    protected IChannelRootDir channelRootDir;
    protected FileTransferStatistic inputStats;
    protected FileTransferStatistic outputStats;

    public PFilePartServerExecutor(IChannelRootDir channelRootDir) {
        this(channelRootDir, null, null);
    }

    public PFilePartServerExecutor(IChannelRootDir channelRootDir, FileTransferStatistic inputStats, FileTransferStatistic outputStats) {
        this.channelRootDir = channelRootDir;
        this.setInputStats(inputStats);
        this.setOutputStats(outputStats);
    }

    @Override
    public byte getSignalByte() {
        return ExecutorSignalByte.FILE_PART.getSignal();
    }

    @Override
    public void executeRequest(Channel channel, ByteBuf byteBuf) {
        byte incomingSignal = byteBuf.readByte();
        if (incomingSignal == Type.GET_FILE.signal) {
            Path uploadPath = this.getUploadFromPath(MyByteBufUtil.getString(byteBuf), channel);
            if (Files.exists(uploadPath)) {
                try {
                    if (Files.isDirectory(uploadPath)) {
                        this.sendDir(uploadPath, channel);
                    } else {
                        this.sendFilePart(uploadPath, byteBuf.readLong(), channel);
                    }
                } catch (Exception e) {
                    this.sendError(channel);
                }
            }
        }
    }

    @Override
    public void executeResponse(Channel channel, ByteBuf byteBuf) {
        byte incomingSignal = byteBuf.readByte();
        try {
            if (incomingSignal == Type.FILE_PART.signal) {
                this.saveFilePart(new PFileInfo(byteBuf), byteBuf, channel);
            } else if (incomingSignal == Type.DIR.signal) {
                this.saveDir(new PFileInfo(byteBuf), byteBuf, channel);
            } else {
                this.sendError(channel);
            }
        } catch (IOException e) {
            this.sendError(channel);
        }
    }

    protected void saveDir(PFileInfo pFileInfo, ByteBuf dirData, Channel channel) throws IOException {
        Path filePath = this.getLoadToPath(pFileInfo.getName(), channel);
        Files.createDirectories(filePath);
        this.sendFinish(channel);
    }

    protected void saveFilePart(PFileInfo pFileInfo, ByteBuf fileData, Channel channel) throws IOException {
        Path filePath = this.getLoadToPath(pFileInfo.getName(), channel);
        Path tempFileName = Paths.get(filePath.toString() + PFilePartServerExecutor.LOADING_FILE_POSTFIX);

        if (Files.notExists(tempFileName)) {
            Files.createDirectories(tempFileName.getParent());
            Files.createFile(tempFileName);
        }
        long seed = MyByteBufUtil.saveFilePart(tempFileName, fileData);

        this.inputStats.take(filePath, pFileInfo.getLength(), seed);

        if (seed < pFileInfo.getLength()) {
            this.requestNextFilePart(pFileInfo.getName(), seed, channel);
        } else {
            Files.deleteIfExists(filePath);
            Files.move(tempFileName, filePath);
            Files.setLastModifiedTime(filePath, FileTime.fromMillis(pFileInfo.getLastModify()));
            this.sendFinish(channel);
        }
    }

    protected void requestNextFilePart(String fileName, long seed, Channel channel) {
        ByteBuf outBuff = channel.alloc().buffer(3 + MyByteBufUtil.getTextLength(fileName) + TypeLengthUtilInByte.LONG_LENGTH);
        outBuff.writeByte(this.getSignalByte());
        outBuff.writeByte(MessageType.REQUEST.signal);
        outBuff.writeByte(Type.GET_FILE.signal);

        MyByteBufUtil.addString(fileName, outBuff);
        outBuff.writeLong(seed);
        channel.writeAndFlush(outBuff);
    }

    protected void sendFilePart(Path filePathFrom, long seed, Channel channel) throws IOException {
        ByteBuf outBuff = channel.alloc().buffer(PFilePartServerExecutor.FILE_PART_BUFFER_LENGTH);
        ByteBuf fileInfoBuff = PFileInfo.getByte(filePathFrom.toFile(), this.getUploadOutcomePath(filePathFrom, channel));
        try {
            outBuff.writeByte(this.getSignalByte());
            outBuff.writeByte(MessageType.RESPONSE.signal);
            outBuff.writeByte(Type.FILE_PART.signal);
            outBuff.writeBytes(fileInfoBuff);

            int buffWithoutData = outBuff.readableBytes();
            MyByteBufUtil.writeFilePart(filePathFrom, seed, outBuff);
            this.outputStats.take(filePathFrom, filePathFrom.toFile().length(), seed + outBuff.readableBytes() - buffWithoutData - TypeLengthUtilInByte.LONG_LENGTH);
            channel.writeAndFlush(outBuff);
        } finally {
            ReferenceCountUtil.release(fileInfoBuff);
        }
    }

    protected void sendDir(Path fileFrom, Channel channel) throws IOException {
        List<String> filesList = Files.list(fileFrom).map(path -> path.subpath(fileFrom.getNameCount(), path.getNameCount()).toString()).collect(Collectors.toList());
        ByteBuf outBuf = channel.alloc().buffer(3 + PFileInfo.countLengthInByte(fileFrom.toString()) + MyByteBufUtil.getTextLength(filesList));
        ByteBuf fileInfoBuff = PFileInfo.getByte(fileFrom.toFile(), this.getUploadOutcomePath(fileFrom, channel));
        try {
            outBuf.writeByte(this.getSignalByte());
            outBuf.writeByte(MessageType.RESPONSE.signal);
            outBuf.writeByte(Type.DIR.signal);

            outBuf.writeBytes(fileInfoBuff);

            MyByteBufUtil.addListOfString(filesList, outBuf);
            channel.writeAndFlush(outBuf);
        } finally {
            ReferenceCountUtil.release(fileInfoBuff);
        }
    }

    public void setChannelRootDir(IChannelRootDir channelRootDir) {
        this.channelRootDir = (channelRootDir == null) ? (ch) -> "." : channelRootDir;
    }

    protected Path getLoadToPath(String fileName, Channel channel) {
        return Paths.get(this.channelRootDir.getRootDir(channel), fileName);
    }

    protected Path getUploadFromPath(String fileName, Channel channel) {
        return this.getLoadToPath(fileName, channel);
    }

    protected String getUploadOutcomePath(Path fileName, Channel channel) {
        return fileName.subpath(Paths.get(this.channelRootDir.getRootDir(channel)).getNameCount(), fileName.getNameCount()).toString();
    }

    public void setInputStats(FileTransferStatistic inputStats) {
        this.inputStats = (inputStats == null) ? (fileName, fileFullLength, FileTransferLength) -> {
        } : inputStats;
    }

    public void setOutputStats(FileTransferStatistic outputStats) {
        this.outputStats = (outputStats == null) ? (fileName, fileFullLength, FileTransferLength) -> {
        } : outputStats;
    }

    //внешние интерфейсы для запроса данных
    public void sendFile(Path filePath, Channel channel) throws IOException {
        if (Files.exists(filePath)) {
            if (filePath.toFile().isFile()) {
                this.sendFilePart(filePath, 0, channel);
            } else {
                this.sendDir(filePath, channel);
            }
        } else {
            System.err.println("PFilePartServerExecutor.sendFile файл не найден: " + filePath);//TODO
        }
    }

    public void getFile(String filePath, Channel channel) {
        ByteBuf outBuf = channel.alloc().buffer(3 + MyByteBufUtil.getTextLength(filePath) + TypeLengthUtilInByte.LONG_LENGTH);
        outBuf.writeByte(this.getSignalByte());
        outBuf.writeByte(MessageType.REQUEST.signal);
        outBuf.writeByte(Type.GET_FILE.signal);

        MyByteBufUtil.addString(filePath, outBuf);
        outBuf.writeLong(0L);
        channel.writeAndFlush(outBuf);
    }

    protected enum Type {
        GET_FILE((byte) 1),
        FILE_PART((byte) 2),
        DIR((byte) 3);
        public final byte signal;

        Type(byte signal) {
            this.signal = signal;
        }
    }
}
