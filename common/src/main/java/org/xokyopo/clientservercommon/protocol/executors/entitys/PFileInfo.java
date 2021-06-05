package org.xokyopo.clientservercommon.protocol.executors.entitys;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.xokyopo.clientservercommon.protocol.MyByteBufUtil;
import org.xokyopo.clientservercommon.protocol.TypeLengthUtilInByte;

import java.io.File;

public class PFileInfo {
    private final String name;
    private final boolean isDir;
    private final long length;
    private final long lastModify;

    public PFileInfo(File file) {
        this.name = file.getName();
        this.isDir = file.isDirectory();
        this.length = file.length();
        this.lastModify = file.lastModified();
    }

    public PFileInfo(ByteBuf byteBuf) {
        this.name = MyByteBufUtil.getString(byteBuf);
        this.isDir = (byteBuf.readByte() == 1);
        this.length = byteBuf.readLong();
        this.lastModify = byteBuf.readLong();
    }

    public static ByteBuf getByte(File file) {
        return getByte(file.getName(), file.isDirectory(), file.length(), file.lastModified());
    }

    public static ByteBuf getByte(File file, String newFileName) {
        return getByte(newFileName, file.isDirectory(), file.length(), file.lastModified());
    }

    public static ByteBuf getByte(String fileName, boolean isDir, long length, long lastModify) {
        ByteBuf result = ByteBufAllocator.DEFAULT.buffer(countLengthInByte(fileName));
        MyByteBufUtil.addString(fileName, result);
        result.writeByte((isDir) ? 1 : 0);
        result.writeLong(length);
        result.writeLong(lastModify);
        return result;
    }

    public static int countLengthInByte(String fileName) {
        return MyByteBufUtil.getTextLength(fileName) + 1 + TypeLengthUtilInByte.LONG_LENGTH * 2;
    }

    public String getName() {
        return name;
    }

    public boolean isDir() {
        return isDir;
    }

    public long getLength() {
        return length;
    }

    public long getLastModify() {
        return lastModify;
    }
}
