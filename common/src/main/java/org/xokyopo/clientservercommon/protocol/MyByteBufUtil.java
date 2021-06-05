package org.xokyopo.clientservercommon.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class MyByteBufUtil {

    public static void addString(String text, ByteBuf byteBuf) {
        ByteBuf buffer = byteBuf.alloc().buffer(getTextLength(text));
        ByteBuf nettyFixBuffer = null;
        try {
            buffer.writeBytes(text.getBytes(StandardCharsets.UTF_8));
            byteBuf.writeInt(buffer.readableBytes());
            nettyFixBuffer = buffer.readBytes(buffer.readableBytes());
            byteBuf.writeBytes(nettyFixBuffer);
        } finally {
            ReferenceCountUtil.release(buffer);
            if (nettyFixBuffer != null) ReferenceCountUtil.release(nettyFixBuffer);
        }
    }


    public static String getString_withError(ByteBuf byteBuf) {
        return byteBuf.readBytes(byteBuf.readInt()).toString(StandardCharsets.UTF_8);
    }

    public static String getString(ByteBuf byteBuf) {
        ByteBuf nettyFixBuffer = byteBuf.readBytes(byteBuf.readInt());
        String result = nettyFixBuffer.toString(StandardCharsets.UTF_8);
        nettyFixBuffer.release();
        return result;
    }

    public static int getTextLength(String... msg) {
        return Arrays.stream(msg).mapToInt(text -> text.length() * TypeLengthUtilInByte.CHAR_LENGTH).sum() + msg.length * TypeLengthUtilInByte.INT_LENGTH;
    }

    public static int getTextLength(List<String> msg) {
        return msg.stream().mapToInt(text -> text.length() * TypeLengthUtilInByte.CHAR_LENGTH).sum() + msg.size() * TypeLengthUtilInByte.INT_LENGTH;
    }

    public static void writeFilePart(Path filePath, long seed, ByteBuf outBuf) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(filePath, READ)) {
            outBuf.writeLong(seed);
            outBuf.writeBytes(fileChannel, seed, outBuf.capacity() - outBuf.readableBytes());
        }
    }

    public static long saveFilePart(Path filePath, ByteBuf inBuf) throws IOException {
        long seed = inBuf.readLong();
        long currentFileLength = filePath.toFile().length();
        if (seed == currentFileLength) {
            try (FileChannel fileChannel = FileChannel.open(filePath, WRITE)) {
                seed += inBuf.readBytes(fileChannel, seed, inBuf.readableBytes());
            }
        } else {
            seed = currentFileLength;
        }
        return seed;
    }

    public static void writeByteBufList(List<ByteBuf> byteBufList, ByteBuf outByteBuf) {
        byteBufList.forEach(byteBuf -> {
            outByteBuf.writeBytes(byteBuf);
            byteBuf.release();
        });
        byteBufList.clear();
    }

    public static int getByteBufListLength(List<ByteBuf> byteBufList) {
        return byteBufList.stream().mapToInt(ByteBuf::readableBytes).sum();
    }


    public static void addListOfString(List<String> stringList, ByteBuf out) {
        stringList.forEach(text -> addString(text, out));
    }

    public static List<String> getListOfString(ByteBuf inBuf) {
        List<String> result = new ArrayList<>();
        while (inBuf.readableBytes() > TypeLengthUtilInByte.INT_LENGTH) {
            result.add(getString(inBuf));
        }
        return result;
    }
}
