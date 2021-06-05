package org.xokyopo.clientservercommon.protocol.executors.entitys;

import java.util.Objects;

public class QueueElement {
    public final String sources;
    public final String dest;

    public QueueElement(String sources, String dest) {
        Objects.requireNonNull(sources);
        Objects.requireNonNull(dest);

        this.sources = sources;
        this.dest = dest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueElement that = (QueueElement) o;
        return sources.equals(that.sources) &&
                dest.equals(that.dest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sources, dest);
    }
}
