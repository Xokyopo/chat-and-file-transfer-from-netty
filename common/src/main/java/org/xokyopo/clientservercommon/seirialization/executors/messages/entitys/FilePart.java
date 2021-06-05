package org.xokyopo.clientservercommon.seirialization.executors.messages.entitys;

import java.io.Serializable;

public class FilePart implements Serializable {
    private final Long fileLength;
    private final Long partSeed;
    private final Object byteArrPart;

    public FilePart(Long fileLength, Long partSeed, byte[] byteArrPart) {
        this.fileLength = fileLength;
        this.partSeed = partSeed;
        this.byteArrPart = byteArrPart;
    }

    public Long getFileLength() {
        return this.fileLength;
    }

    public Long getPartSeed() {
        return this.partSeed;
    }

    public byte[] getByteArrPart() {
        return (byte[]) this.byteArrPart;
    }
}
