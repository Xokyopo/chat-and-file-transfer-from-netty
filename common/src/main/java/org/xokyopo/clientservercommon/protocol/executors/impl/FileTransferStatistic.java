package org.xokyopo.clientservercommon.protocol.executors.impl;

import java.nio.file.Path;

public interface FileTransferStatistic {
    void take(Path fileName, long fileFullLength, long FileTransmittedLength);
}
