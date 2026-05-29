package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RecipeIndexIds {

    private RecipeIndexIds() {
    }

    record RecipeIdParts(String namespace, String path) {
    }

    static List<String> allRecipeIds(Path outputDir, RecipeBundleMods mods) throws IOException {
        if (mods == null || mods.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, RecipeBundleMods.ModEntry> entry : mods.mods().entrySet()) {
            String namespace = entry.getKey();
            for (String routeFile : entry.getValue().routes()) {
                ids.addAll(readRouteShardPaths(outputDir, namespace, routeFile));
            }
        }
        return ids;
    }

    static JsonObject loadLayout(Path outputDir, String recipeId, RecipeBundleMods mods) throws IOException {
        RecipeIdParts parts = splitRecipeId(recipeId);
        if (parts == null || mods == null) {
            return null;
        }
        RecipeBundleMods.ModEntry mod = mods.mods().get(parts.namespace());
        if (mod == null) {
            return null;
        }
        Integer packIndex = findPackIndex(outputDir, parts.namespace(), mod, parts.path());
        if (packIndex == null || packIndex < 0 || packIndex >= mod.packs().size()) {
            return null;
        }
        RecipeBundleMods.PackRef packRef = mod.packs().get(packIndex);
        Path packPath = EmiBundlePaths.resolve(
                outputDir,
                EmiBundlePaths.RECIPES_LAYOUT_PACKS_DIR + "/" + parts.namespace() + "/" + packRef.file() + ".json");
        if (!Files.isRegularFile(packPath)) {
            throw new IOException("missing layout pack file: " + packPath);
        }
        JsonObject pack = JsonParser.parseString(Files.readString(packPath)).getAsJsonObject();
        JsonElement layout = pack.getAsJsonObject("layouts").get(parts.path());
        return layout != null && layout.isJsonObject() ? layout.getAsJsonObject() : null;
    }

    private static Integer findPackIndex(
            Path outputDir,
            String namespace,
            RecipeBundleMods.ModEntry mod,
            String path) throws IOException {
        for (String routeFile : mod.routes()) {
            Path routePath = EmiBundlePaths.resolve(
                    outputDir,
                    EmiBundlePaths.RECIPES_ROUTES_DIR + "/" + namespace + "/" + routeFile + ".json");
            if (!Files.isRegularFile(routePath)) {
                throw new IOException("missing route shard file: " + routePath);
            }
            JsonObject routeShard = JsonParser.parseString(Files.readString(routePath)).getAsJsonObject();
            JsonObject routes = routeShard.getAsJsonObject("routes");
            if (routes != null && routes.has(path)) {
                return routes.get(path).getAsInt();
            }
        }
        return null;
    }

    private static List<String> readRouteShardPaths(Path outputDir, String namespace, String routeFile)
            throws IOException {
        Path routePath = EmiBundlePaths.resolve(
                outputDir,
                EmiBundlePaths.RECIPES_ROUTES_DIR + "/" + namespace + "/" + routeFile + ".json");
        if (!Files.isRegularFile(routePath)) {
            throw new IOException("missing route shard file: " + routePath);
        }
        JsonObject routeShard = JsonParser.parseString(Files.readString(routePath)).getAsJsonObject();
        JsonObject routes = routeShard.getAsJsonObject("routes");
        if (routes == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : routes.entrySet()) {
            ids.add(namespace + ":" + entry.getKey());
        }
        return ids;
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

    /** Test helper: write route + pack for a single layout without running the full exporter. */
    static RecipeBundleMods writeFixtureLayout(Path outputDir, String recipeId, JsonObject layout)
            throws IOException {
        RecipeRoutePackWriter writer = new RecipeRoutePackWriter(outputDir, RecipeRoutePackWriter.defaultPackMaxBytes());
        writer.addLayout(recipeId, layout);
        return writer.finish();
    }
}
