package io.github.jmecn.minecraftwebexport.io;

import java.io.IOException;
import java.nio.file.Path;

public interface BundleWriteStrategy extends AutoCloseable {

    String profileId();

    void open(Path exportRoot) throws IOException;

    void writeBytes(Path bundleAbsolutePath, byte[] data) throws IOException;

    void writeJson(Path bundleAbsolutePath, Object value) throws IOException;

    void writeJsonDocument(Path bundleAbsolutePath, Object value) throws IOException;

    void finish() throws IOException;

    boolean usesFileTree();

    int preferredWorkerCount();

    @Override
    void close();
}
