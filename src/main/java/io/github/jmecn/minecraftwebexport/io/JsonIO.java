package io.github.jmecn.minecraftwebexport.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final class ItemIndexTypeAdapter extends TypeAdapter<ItemIndex> {

        @Override
        public void write(JsonWriter out, ItemIndex value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("schema").value(Constants.ITEM_INDEX_SCHEMA);
            for (Map.Entry<String, List<String>> entry : value.namespacePaths().entrySet()) {
                out.name(entry.getKey());
                out.beginArray();
                for (String path : entry.getValue()) {
                    out.value(path);
                }
                out.endArray();
            }
            if (!value.fluidRegistryIds().isEmpty()) {
                out.name(Constants.FLUID_REGISTRY_IDS_KEY);
                out.beginArray();
                for (String fluidId : value.fluidRegistryIds()) {
                    out.value(fluidId);
                }
                out.endArray();
            }
            out.endObject();
        }

        @Override
        public ItemIndex read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            Map<String, List<String>> namespaces = new LinkedHashMap<>();
            List<String> fluids = List.of();
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if ("schema".equals(key)) {
                    in.skipValue();
                    continue;
                }
                if (Constants.FLUID_REGISTRY_IDS_KEY.equals(key)) {
                    fluids = readStringList(in);
                    continue;
                }
                namespaces.put(key, readStringList(in));
            }
            in.endObject();
            return new ItemIndex(namespaces, fluids);
        }

        private static List<String> readStringList(JsonReader in) throws IOException {
            List<String> values = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()) {
                values.add(in.nextString());
            }
            in.endArray();
            return List.copyOf(values);
        }
    }
}
