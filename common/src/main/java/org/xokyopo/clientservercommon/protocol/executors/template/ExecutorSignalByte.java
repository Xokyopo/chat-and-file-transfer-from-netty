package org.xokyopo.clientservercommon.protocol.executors.template;

public enum ExecutorSignalByte {
    ONE_STRING((byte) 0),
    FILE_LIST((byte) 1),
    FILE_OPERATION((byte) 2),
    FILE_PART((byte) 3),
    AUTHORIZATION((byte) 4);

    private final byte signal;

    ExecutorSignalByte(byte signal) {
        this.signal = signal;
    }

    public byte getSignal() {
        return signal;
    }
}
