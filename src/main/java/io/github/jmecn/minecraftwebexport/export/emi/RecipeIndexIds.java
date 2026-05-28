package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RecipeIndexIds {

    private RecipeIndexIds() {
    }

    record RecipeIdParts(String namespace, String path) {
    }

    static List<String> read(Path outputDir, JsonObject recipeIndex) throws IOException {
        JsonElement namespacesElement = recipeIndex.get("namespaces");
        if (namespacesElement == null || !namespacesElement.isJsonArray()) {
            return Collections.emptyList();
        }
        JsonArray namespaces = namespacesElement.getAsJsonArray();
        List<String> ids = new ArrayList<>();
        for (JsonElement namespaceElement : namespaces) {
            String namespace = readNonBlankString(namespaceElement);
            if (namespace == null) {
                continue;
            }
            ids.addAll(readShardRecipeIds(outputDir, namespace));
        }
        return ids;
    }

    private static List<String> readShardRecipeIds(Path outputDir, String namespace) throws IOException {
        Path shardPath = EmiBundlePaths.resolve(
                outputDir,
                EmiBundlePaths.RECIPE_SHARDS_DIR + "/" + namespace + ".json");
        if (!Files.isRegularFile(shardPath)) {
            throw new IOException("missing recipe shard file: " + shardPath);
        }

        JsonElement shardElement = com.google.gson.JsonParser.parseString(Files.readString(shardPath));
        if (!shardElement.isJsonArray()) {
            throw new IOException("invalid recipe shard file (must be array): " + shardPath);
        }

        List<String> ids = new ArrayList<>();
        for (JsonElement pathElement : shardElement.getAsJsonArray()) {
            String path = readNonBlankString(pathElement);
            if (path != null) {
                ids.add(namespace + ":" + path);
            }
        }
        return ids;
    }

    private static String readNonBlankString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    static RecipeIdParts splitRecipeId(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return null;
        }
        int sep = recipeId.indexOf(':');
        if (sep <= 0 || sep >= recipeId.length() - 1) {
            return null;
        }
        return new RecipeIdParts(recipeId.substring(0, sep), recipeId.substring(sep + 1));
    }
}
