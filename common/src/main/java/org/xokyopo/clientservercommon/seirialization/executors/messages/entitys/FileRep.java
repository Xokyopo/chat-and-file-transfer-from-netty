package org.xokyopo.clientservercommon.seirialization.executors.messages.entitys;

import java.io.File;
import java.io.Serializable;

public class FileRep implements Serializable {
    private final String name;
    private final String path;
    private final Boolean isDir;
    private final Long size;

    public FileRep(File file) {
        this.name = file.getName();
        this.path = file.getAbsolutePath();
        this.isDir = file.isDirectory();
        this.size = file.length();
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Boolean getDir() {
        return isDir;
    }

    public Long getSize() {
        return size;
    }
}
