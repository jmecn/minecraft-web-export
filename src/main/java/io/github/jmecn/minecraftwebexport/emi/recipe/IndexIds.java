package io.github.jmecn.minecraftwebexport.emi.recipe;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import io.github.jmecn.minecraftwebexport.emi.recipe.LayoutLookup;
import io.github.jmecn.minecraftwebexport.emi.recipe.RoutePackWriter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class IndexIds {

    private IndexIds() {
    }

    record RecipeIdParts(String namespace, String path) {
    }

    public static List<String> allRecipeIds(Path outputDir, BundleMods mods) throws IOException {
        if (mods == null || mods.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, BundleMods.ModEntry> entry : mods.mods().entrySet()) {
            String namespace = entry.getKey();
            for (String routeFile : entry.getValue().routes()) {
                ids.addAll(readRouteShardPaths(outputDir, namespace, routeFile));
            }
        }
        return ids;
    }

    public static JsonObject loadLayout(Path outputDir, String recipeId, BundleMods mods) throws IOException {
        return new LayoutLookup(outputDir, mods).loadLayout(recipeId);
    }

    private static List<String> readRouteShardPaths(Path outputDir, String namespace, String routeFile)
            throws IOException {
        Path routePath = Paths.resolve(
                outputDir,
                Paths.RECIPES_ROUTES_DIR + "/" + namespace + "/" + routeFile + ".json");
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

    public static BundleMods writeFixtureLayout(Path outputDir, String recipeId, JsonObject layout)
            throws IOException {
        RoutePackWriter writer = new RoutePackWriter(outputDir, RoutePackWriter.defaultPackMaxBytes());
        writer.addLayout(recipeId, layout);
        return writer.finish();
    }
}
