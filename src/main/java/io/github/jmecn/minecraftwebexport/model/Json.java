package io.github.jmecn.minecraftwebexport.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Json {

    public static final Gson GSON = new GsonBuilder().create();

    private Json() {}

    public static void write(Path path, Object document) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(document, "document");
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(document), StandardCharsets.UTF_8);
    }

    public static <T> T read(Path path, Class<T> type) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
        return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
    }
}
