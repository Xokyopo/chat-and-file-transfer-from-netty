package org.xokyopo.clientservercommon.seirialization.executors.messages;

import org.xokyopo.clientservercommon.seirialization.executors.messages.entitys.FilePart;
import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

public class FilePartMessage extends AbstractMessage {
    private final String fileName;
    private final String targetPath;
    private final String destPath;
    private final Boolean isDir;
    private final Long lastModified;
    private final FilePart filePart;

    public FilePartMessage(Type type, String fileName, String targetPath, String destPath, Boolean isDir, Long lastModified) {
        this(type, fileName, targetPath, destPath, isDir, lastModified, null);
    }

    public FilePartMessage(Type type, String fileName, String targetPath, String destPath, Boolean isDir, Long lastModified, FilePart filePart) {
        super(type);
        this.fileName = fileName;
        this.targetPath = targetPath;
        this.destPath = destPath;
        this.isDir = isDir;
        this.lastModified = lastModified;
        this.filePart = filePart;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getDestPath() {
        return destPath;
    }

    public Boolean isDir() {
        return isDir;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public FilePart getFilePart() {
        return filePart;
    }
}
