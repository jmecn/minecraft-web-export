package io.github.jmecn.minecraftwebexport.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class BundleFileWrites {

    static final int BUFFER_SIZE = 1024 * 1024;

    private BundleFileWrites() {
    }

    static void writeBytes(Path path, byte[] data) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path), BUFFER_SIZE)) {
            out.write(data);
        }
    }

    static void writeJson(Path path, Object value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedOutputStream bytes = new BufferedOutputStream(Files.newOutputStream(path), BUFFER_SIZE);
                Writer writer = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
            JsonIO.GSON.toJson(value, writer);
        }
    }
}
