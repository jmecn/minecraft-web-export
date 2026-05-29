package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import io.github.jmecn.minecraftwebexport.export.ExportGson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class RecipeLayoutIndexWriter {

    /** {@code recipes/index.json} contract schema (not per-layout JSON schema). */
    public static final int INDEX_SCHEMA_VERSION = 1;

    private static final Gson GSON = ExportGson.GSON;

    private RecipeLayoutIndexWriter() {
    }

    public static void write(Path outputDir, int scale, Collection<String> recipeIds) throws IOException {
        Map<String, TreeSet<String>> pathsByNamespace = collectPathsByNamespace(recipeIds);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", INDEX_SCHEMA_VERSION);
        root.put("scale", scale);
        root.put("namespaces", List.copyOf(pathsByNamespace.keySet()));

        Path out = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.LAYOUT_INDEX_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root));

        Path shardsDir = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.RECIPE_SHARDS_DIR);
        Files.createDirectories(shardsDir);
        for (Map.Entry<String, TreeSet<String>> entry : pathsByNamespace.entrySet()) {
            Path shardFile = shardsDir.resolve(entry.getKey() + ".json");
            Files.writeString(shardFile, GSON.toJson(List.copyOf(entry.getValue())));
        }
    }

    private static Map<String, TreeSet<String>> collectPathsByNamespace(Collection<String> recipeIds) {
        Map<String, TreeSet<String>> pathsByNamespace = new TreeMap<>();
        for (String recipeId : recipeIds) {
            RecipeIndexIds.RecipeIdParts parts = RecipeIndexIds.splitRecipeId(recipeId);
            if (parts == null) {
                continue;
            }
            pathsByNamespace
                    .computeIfAbsent(parts.namespace(), ignored -> new TreeSet<>())
                    .add(parts.path());
        }
        return pathsByNamespace;
    }
}
