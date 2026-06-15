package io.github.jmecn.minecraftwebexport.io;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipWriteStrategy implements BundleWriteStrategy {

    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final int JSON_DEFLATE_LEVEL = 5;

    private Path bundleRoot;
    private Path zipPath;
    private Path partPath;
    private ZipOutputStream zipOut;
    private int entryCount;

    @Override
    public String profileId() {
        return "zip";
    }

    @Override
    public synchronized void open(Path exportRoot) throws IOException {
        bundleRoot = BundleEntryPaths.bundleRoot(exportRoot);
        zipPath = BundleEntryPaths.zipFile(exportRoot);
        partPath = exportRoot.resolve(Constants.EMI_ZIP_FILE + ".part");
        Files.createDirectories(exportRoot);
        Files.deleteIfExists(partPath);
        Files.deleteIfExists(zipPath);
        zipOut = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(partPath), BUFFER_SIZE));
        entryCount = 0;
        MweMod.LOGGER.info("{} writing {}", Log.EMI, zipPath.toAbsolutePath());
    }

    @Override
    public synchronized void writeBytes(Path bundleAbsolutePath, byte[] data) throws IOException {
        putEntry(bundleAbsolutePath, data, entryMethod(bundleAbsolutePath));
    }

    @Override
    public synchronized void writeJson(Path bundleAbsolutePath, Object value) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
            JsonIO.GSON.toJson(value, writer);
        }
        putEntry(bundleAbsolutePath, buffer.toByteArray(), ZipEntry.DEFLATED);
    }

    @Override
    public synchronized void writeJsonDocument(Path bundleAbsolutePath, Object value) throws IOException {
        writeJson(bundleAbsolutePath, value);
    }

    @Override
    public synchronized void finish() throws IOException {
        if (zipOut == null) {
            return;
        }
        zipOut.flush();
        zipOut.close();
        zipOut = null;
        Files.move(partPath, zipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        long zipBytes = Files.size(zipPath);
        MweMod.LOGGER.info(
                "{} {} complete: {} entries, {} zip bytes",
                Log.EMI,
                zipPath.getFileName(),
                entryCount,
                zipBytes);
    }

    @Override
    public boolean usesFileTree() {
        return false;
    }

    @Override
    public int preferredWorkerCount() {
        return 1;
    }

    @Override
    public synchronized void close() {
        if (zipOut != null) {
            try {
                zipOut.close();
            } catch (IOException ignored) {
            }
            zipOut = null;
        }
        try {
            Files.deleteIfExists(partPath);
        } catch (IOException ignored) {
        }
    }

    private void putEntry(Path bundleAbsolutePath, byte[] data, int method) throws IOException {
        if (zipOut == null) {
            throw new IllegalStateException("zip export not open");
        }
        String entryName = BundleEntryPaths.entryName(bundleRoot, bundleAbsolutePath);
        ZipEntry entry = new ZipEntry(entryName);
        if (method == ZipEntry.STORED) {
            CRC32 crc = new CRC32();
            crc.update(data);
            entry.setSize(data.length);
            entry.setCrc(crc.getValue());
            entry.setMethod(ZipEntry.STORED);
        } else {
            entry.setMethod(ZipEntry.DEFLATED);
            zipOut.setLevel(JSON_DEFLATE_LEVEL);
        }
        zipOut.putNextEntry(entry);
        zipOut.write(data);
        zipOut.closeEntry();
        entryCount++;
    }

    private static int entryMethod(Path bundleAbsolutePath) {
        String name = bundleAbsolutePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json") || name.endsWith(".css")) {
            return ZipEntry.DEFLATED;
        }
        return ZipEntry.STORED;
    }
}
