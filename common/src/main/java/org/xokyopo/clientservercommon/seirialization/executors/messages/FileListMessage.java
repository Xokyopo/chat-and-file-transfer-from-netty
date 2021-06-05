package org.xokyopo.clientservercommon.seirialization.executors.messages;

import org.xokyopo.clientservercommon.seirialization.executors.messages.entitys.FileRep;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

import java.util.List;

public class FileListMessage extends AbstractMessage {
    private final List<FileRep> fileList;
    private final String path;

    public FileListMessage(AbstractMessage.Type type, List<FileRep> fileList, String path) {
        super(type);
        this.fileList = fileList;
        this.path = path;
    }

    public List<FileRep> getFileList() {
        return fileList;
    }

    public String getPath() {
        return path;
    }
}
