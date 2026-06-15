package io.github.jmecn.minecraftwebexport.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes EMI bundle entries to a file tree ({@link FileWriteStrategy}) or {@code emi.zip}
 * ({@link ZipWriteStrategy}).
 */
public interface BundleWriteStrategy extends AutoCloseable {

    /** {@code file} or {@code zip}. */
    String profileId();

    /** Prepare the write target under {@code exportRoot}. */
    void open(Path exportRoot) throws IOException;

    void writeBytes(Path bundleAbsolutePath, byte[] data) throws IOException;

    void writeJson(Path bundleAbsolutePath, Object value) throws IOException;

    void writeJsonDocument(Path bundleAbsolutePath, Object value) throws IOException;

    void finish() throws IOException;

    /** When false, callers should skip filesystem prep under {@code emi/}. */
    boolean usesFileTree();

    int preferredWorkerCount();

    @Override
    void close();
}
