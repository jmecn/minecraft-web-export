package io.github.jmecn.minecraftwebexport.io;

import java.io.IOException;
import java.nio.file.Path;

public final class FileWriteStrategy implements BundleWriteStrategy {

    private static final int THREADS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));

    private Path bundleRoot;

    @Override
    public String profileId() {
        return "file";
    }

    @Override
    public void open(Path exportRoot) throws IOException {
        bundleRoot = BundleEntryPaths.bundleRoot(exportRoot);
        java.nio.file.Files.createDirectories(bundleRoot);
    }

    @Override
    public void writeBytes(Path bundleAbsolutePath, byte[] data) throws IOException {
        BundleFileWrites.writeBytes(bundleAbsolutePath, data);
    }

    @Override
    public void writeJson(Path bundleAbsolutePath, Object value) throws IOException {
        BundleFileWrites.writeJson(bundleAbsolutePath, value);
    }

    @Override
    public void writeJsonDocument(Path bundleAbsolutePath, Object value) throws IOException {
        BundleFileWrites.writeJson(bundleAbsolutePath, value);
    }

    @Override
    public void finish() {
    }

    @Override
    public boolean usesFileTree() {
        return true;
    }

    @Override
    public int preferredWorkerCount() {
        return THREADS;
    }

    @Override
    public void close() {
        bundleRoot = null;
    }

    Path bundleRoot() {
        return bundleRoot;
    }
}
