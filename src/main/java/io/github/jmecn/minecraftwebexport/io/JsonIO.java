package io.github.jmecn.minecraftwebexport.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class JsonIO {

    public static final Gson GSON = new GsonBuilder().create();

    private JsonIO() {}

    public static String toJson(Object value) {
        return GSON.toJson(Objects.requireNonNull(value, "value"));
    }

    public static <T> T fromJson(String json, Class<T> type) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(type, "type");
        return GSON.fromJson(json, type);
    }

    public static void write(Path path, Object value) throws IOException {
        Objects.requireNonNull(path, "path");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(value), StandardCharsets.UTF_8);
    }

    public static void write(File file, Object value) throws IOException {
        write(Objects.requireNonNull(file, "file").toPath(), value);
    }

    public static void write(String filename, Object value) throws IOException {
        write(Path.of(Objects.requireNonNull(filename, "filename")), value);
    }

    public static void write(OutputStream out, Object value) throws IOException {
        Objects.requireNonNull(out, "out");
        out.write(toJson(value).getBytes(StandardCharsets.UTF_8));
    }

    public static void write(FileOutputStream out, Object value) throws IOException {
        write((OutputStream) out, value);
    }

    public static void writeLine(Path path, Object value) throws IOException {
        Objects.requireNonNull(path, "path");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                path,
                toJson(value) + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    public static <T> T read(Path path, Class<T> type) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
        return fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
    }

    public static byte[] toUtf8Bytes(Object value) {
        return toJson(value).getBytes(StandardCharsets.UTF_8);
    }
}
