package io.github.jmecn.minecraftwebexport.io;

import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ExportWriteQueue implements AutoCloseable {

    static final int WRITE_MAX_PENDING = 512;

    private final Path exportRoot;
    private final BundleWriteStrategy strategy;
    private final ExecutorService executor;
    private final Semaphore inFlight;
    private final List<Future<?>> pending = new ArrayList<>();
    private final AtomicLong batchIoNanos = new AtomicLong();
    private final AtomicLong batchAcquireNanos = new AtomicLong();
    private long batchSubmitStartedAtNanos;
    private boolean finished;

    private ExportWriteQueue(Path exportRoot, BundleWriteStrategy strategy) {
        this.exportRoot = Objects.requireNonNull(exportRoot, "exportRoot");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.executor = Executors.newFixedThreadPool(strategy.preferredWorkerCount());
        this.inFlight = new Semaphore(WRITE_MAX_PENDING);
    }

    public static ExportWriteQueue create(Path exportRoot, boolean compressed) throws IOException {
        BundleWriteStrategy strategy = BundleWriteStrategies.create(exportRoot, compressed);
        ExportWriteQueue queue = new ExportWriteQueue(exportRoot, strategy);
        strategy.open(exportRoot);
        MweMod.LOGGER.info("{} write profile: {}", Log.EMI, strategy.profileId());
        return queue;
    }

    public static ExportWriteQueue create(Path exportRoot) throws IOException {
        return create(exportRoot, io.github.jmecn.minecraftwebexport.config.MweConfig.exportCompressed());
    }

    public boolean usesFileTree() {
        return strategy.usesFileTree();
    }

    public String writeProfile() {
        return strategy.profileId();
    }

    public void submitJson(Path path, Object value) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        submit(() -> strategy.writeJson(path, value), path);
    }

    public void submitBytes(Path path, byte[] data) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(data, "data");
        submit(() -> strategy.writeBytes(path, data), path);
    }

    public void submitString(Path path, String content) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
        submitBytes(path, content.getBytes(StandardCharsets.UTF_8));
    }

    public void submitJsonLine(Path path, Object value) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        submit(() -> strategy.writeJsonDocument(path, value), path);
    }

    private void submit(WriteTask task, Path path) {
        acquire(path);
        pending.add(executor.submit(() -> {
            long ioStartedAt = System.nanoTime();
            try {
                task.run();
            } catch (IOException e) {
                throw new RuntimeException("failed to write " + path + ": " + e.getMessage(), e);
            } finally {
                batchIoNanos.addAndGet(System.nanoTime() - ioStartedAt);
                inFlight.release();
            }
        }));
    }

    private void acquire(Path path) {
        long acquireStartedAt = System.nanoTime();
        try {
            inFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("export write queue interrupted waiting for slot: " + path, e);
        } finally {
            batchAcquireNanos.addAndGet(System.nanoTime() - acquireStartedAt);
        }
        if (batchSubmitStartedAtNanos == 0L) {
            batchSubmitStartedAtNanos = System.nanoTime();
        }
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
        MweMod.LOGGER.info("{} flushing {} pending writes ({}) ...", Log.EMI, count, label);
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
        long finishedAt = System.nanoTime();
        long mainWaitMs = (finishedAt - startedAt) / 1_000_000L;
        long workerIoMs = batchIoNanos.get() / 1_000_000L;
        long acquireWaitMs = batchAcquireNanos.get() / 1_000_000L;
        long wallMs =
                batchSubmitStartedAtNanos == 0L
                        ? mainWaitMs
                        : (finishedAt - batchSubmitStartedAtNanos) / 1_000_000L;
        batchIoNanos.set(0L);
        batchAcquireNanos.set(0L);
        batchSubmitStartedAtNanos = 0L;
        MweMod.LOGGER.info(
                "{} writes flushed: {} tasks, mainWait={} ms, workerIo={} ms, wall={} ms, acquireWait={} ms ({})",
                Log.EMI,
                count,
                mainWaitMs,
                workerIoMs,
                wallMs,
                acquireWaitMs,
                label);
        if (failure != null) {
            throw failure;
        }
    }

    public static void drain(Path exportRoot, ThrowingConsumer<ExportWriteQueue> action) throws IOException {
        Objects.requireNonNull(action, "action");
        try (ExportWriteQueue queue = ExportWriteQueue.create(exportRoot)) {
            action.accept(queue);
            queue.awaitIdle();
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T value) throws IOException;
    }

    @FunctionalInterface
    private interface WriteTask {
        void run() throws IOException;
    }

    @Override
    public void close() {
        if (!finished) {
            try {
                awaitIdle("close");
                strategy.finish();
                finished = true;
            } catch (IOException e) {
                throw new RuntimeException("failed to finish export writes: " + e.getMessage(), e);
            } finally {
                strategy.close();
            }
        }
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

    Path exportRoot() {
        return exportRoot;
    }
}
