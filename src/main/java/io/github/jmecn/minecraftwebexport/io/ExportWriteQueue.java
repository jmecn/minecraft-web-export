package io.github.jmecn.minecraftwebexport.io;

import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Async disk writes for export. Rendering stays on the Minecraft render thread; JSON, PNG bytes,
 * and other file output are serialized and flushed on a small worker pool.
 */
public final class ExportWriteQueue implements AutoCloseable {

    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final int THREADS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    private final List<Future<?>> pending = new ArrayList<>();

    public void submitJson(Path path, Object value) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        pending.add(executor.submit(() -> {
            try {
                writeJson(path, value);
            } catch (IOException e) {
                throw new RuntimeException("failed to write " + path + ": " + e.getMessage(), e);
            }
        }));
    }

    public void submitBytes(Path path, byte[] data) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(data, "data");
        pending.add(executor.submit(() -> {
            try {
                writeBytes(path, data);
            } catch (IOException e) {
                throw new RuntimeException("failed to write " + path + ": " + e.getMessage(), e);
            }
        }));
    }

    public void submitString(Path path, String content) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
        submitBytes(path, content.getBytes(StandardCharsets.UTF_8));
    }

    public void submitJsonLine(Path path, Object value) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        pending.add(executor.submit(() -> {
            try {
                writeJsonLine(path, value);
            } catch (IOException e) {
                throw new RuntimeException("failed to write " + path + ": " + e.getMessage(), e);
            }
        }));
    }

    public void awaitIdle() {
        awaitIdle(null);
    }

    public void awaitIdle(String phase) {
        int count = pending.size();
        if (count == 0) {
            return;
        }
        String label = phase == null || phase.isBlank() ? "export" : phase;
        MweMod.LOGGER.info("{} flushing {} pending disk writes ({}) ...", Log.EMI, count, label);
        long startedAt = System.nanoTime();
        RuntimeException failure = null;
        for (Future<?> future : pending) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failure = new RuntimeException("export write queue interrupted", e);
            } catch (Exception e) {
                failure = new RuntimeException("export write failed: " + e.getMessage(), e);
            }
        }
        pending.clear();
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        MweMod.LOGGER.info(
                "{} disk writes flushed: {} tasks, {} ms ({})",
                Log.EMI,
                count,
                elapsedMs,
                label);
        if (failure != null) {
            throw failure;
        }
    }

    public static long writeJson(Path path, Object value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedOutputStream bytes = new BufferedOutputStream(Files.newOutputStream(path), BUFFER_SIZE);
                Writer writer = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
            JsonIO.GSON.toJson(value, writer);
        }
        return Files.size(path);
    }

    public static long writeJsonLine(Path path, Object value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String line = JsonIO.toJson(value) + "\n";
        Files.writeString(
                path,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        return line.getBytes(StandardCharsets.UTF_8).length;
    }

    public static long writeBytes(Path path, byte[] data) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path), BUFFER_SIZE)) {
            out.write(data);
        }
        return data.length;
    }

    /** Runs {@code action} with a fresh queue and blocks until all submitted writes finish. */
    public static void drain(ThrowingConsumer<ExportWriteQueue> action) throws IOException {
        Objects.requireNonNull(action, "action");
        try (ExportWriteQueue queue = new ExportWriteQueue()) {
            action.accept(queue);
            queue.awaitIdle();
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T value) throws IOException;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                MweMod.LOGGER.warn("{} export write queue did not finish within timeout", Log.EMI);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
