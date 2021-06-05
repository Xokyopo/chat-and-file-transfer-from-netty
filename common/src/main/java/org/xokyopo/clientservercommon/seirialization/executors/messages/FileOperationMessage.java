package org.xokyopo.clientservercommon.seirialization.executors.messages;

import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

public class FileOperationMessage extends AbstractMessage {
    private final String fileName;
    private final OType oType;
    private final String newFileName;

    public FileOperationMessage(AbstractMessage.Type type, OType type1, String fileName, String newFileName) {
        super(type);
        this.fileName = fileName;
        this.oType = type1;
        this.newFileName = newFileName;
    }

    public FileOperationMessage.OType getOType() {
        return this.oType;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getNewFileName() {
        return this.newFileName;
    }

    public enum OType {DELETE, COPY, MOVE}
}
