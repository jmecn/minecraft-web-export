package io.github.jmecn.minecraftwebexport.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class PendingWriteBuffer {

    private record Entry(Path path, byte[] data) {}

    private final Object lock = new Object();
    private final List<Entry> entries = new ArrayList<>();

    void addJson(Path path, Object value) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        byte[] data = JsonIO.toUtf8Bytes(value);
        synchronized (lock) {
            entries.add(new Entry(path, data));
        }
    }

    int size() {
        synchronized (lock) {
            return entries.size();
        }
    }

    int drainTo(ExportWriteQueue queue, int max) {
        if (max <= 0) {
            return 0;
        }
        List<Entry> batch;
        synchronized (lock) {
            if (entries.isEmpty()) {
                return 0;
            }
            int count = Math.min(max, entries.size());
            batch = new ArrayList<>(entries.subList(0, count));
            entries.subList(0, count).clear();
        }
        for (Entry entry : batch) {
            queue.submitBytesDirect(entry.path, entry.data);
        }
        return batch.size();
    }

    int drainAll(ExportWriteQueue queue) {
        return drainTo(queue, Integer.MAX_VALUE);
    }
}
