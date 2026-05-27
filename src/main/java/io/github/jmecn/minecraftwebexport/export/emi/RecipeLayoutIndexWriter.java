package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class RecipeLayoutIndexWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RecipeLayoutIndexWriter() {
    }

    public record Entry(String layout, String category, String reference) {
    }

    public static void write(Path outputDir, int scale, Map<String, Entry> entries) throws IOException {
        Map<String, Object> recipes = new TreeMap<>();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("layout", entry.getValue().layout());
            if (entry.getValue().category() != null) {
                value.put("category", entry.getValue().category());
            }
            if (entry.getValue().reference() != null) {
                value.put("reference", entry.getValue().reference());
            }
            recipes.put(entry.getKey(), value);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", RecipeLayoutPaths.SCHEMA_VERSION);
        root.put("scale", scale);
        root.put("recipes", recipes);

        Path out = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.LAYOUT_INDEX_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root));
    }
}
