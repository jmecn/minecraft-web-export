package io.github.jmecn.minecraftwebexport.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.minecraftwebexport.io.gson.ItemIndexTypeAdapter;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class JsonIO {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ItemIndex.class, new ItemIndexTypeAdapter())
            .create();

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

    public static <T> T read(Path path, Class<T> type) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
        return fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
    }

    public static byte[] toUtf8Bytes(Object value) {
        return toJson(value).getBytes(StandardCharsets.UTF_8);
    }
}
