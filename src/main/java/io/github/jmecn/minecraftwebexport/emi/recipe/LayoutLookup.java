package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.ModEntry;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.PackRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class LayoutLookup {

    private final Path outputDir;
    private final BundleMods mods;
    private final Map<String, JsonObject> routeShards = new HashMap<>();
    private final Map<String, JsonObject> layoutPacks = new HashMap<>();

    LayoutLookup(Path outputDir, BundleMods mods) {
        this.outputDir = outputDir;
        this.mods = mods;
    }

    JsonObject loadLayout(String recipeId) throws IOException {
        IndexIds.RecipeIdParts parts = IndexIds.splitRecipeId(recipeId);
        if (parts == null || mods == null) {
            return null;
        }
        ModEntry mod = mods.mods().get(parts.namespace());
        if (mod == null) {
            return null;
        }
        Integer packIndex = findPackIndex(parts.namespace(), mod, parts.path());
        if (packIndex == null || packIndex < 0 || packIndex >= mod.packs().size()) {
            return null;
        }
        PackRef packRef = mod.packs().get(packIndex);
        JsonObject pack = loadLayoutPack(parts.namespace(), packRef.file());
        JsonElement layout = pack.getAsJsonObject("layouts").get(parts.path());
        return layout != null && layout.isJsonObject() ? layout.getAsJsonObject() : null;
    }

    private Integer findPackIndex(String namespace, ModEntry mod, String path)
            throws IOException {
        for (String routeFile : mod.routes()) {
            JsonObject routeShard = loadRouteShard(namespace, routeFile);
            JsonObject routes = routeShard.getAsJsonObject("routes");
            if (routes != null && routes.has(path)) {
                return routes.get(path).getAsInt();
            }
        }
        return null;
    }

    private JsonObject loadRouteShard(String namespace, String routeFile) throws IOException {
        String cacheKey = namespace + "/" + routeFile;
        JsonObject cached = routeShards.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Path routePath = EmiPaths.resolve(
                outputDir,
                Constants.RECIPES_ROUTES_DIR + "/" + namespace + "/" + routeFile + ".json");
        if (!Files.isRegularFile(routePath)) {
            throw new IOException("missing route shard file: " + routePath);
        }
        JsonObject routeShard = JsonParser.parseString(Files.readString(routePath)).getAsJsonObject();
        routeShards.put(cacheKey, routeShard);
        return routeShard;
    }

    private JsonObject loadLayoutPack(String namespace, String packFile) throws IOException {
        String cacheKey = namespace + "/" + packFile;
        JsonObject cached = layoutPacks.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Path packPath = EmiPaths.resolve(
                outputDir,
                Constants.RECIPES_LAYOUT_PACKS_DIR + "/" + namespace + "/" + packFile + ".json");
        if (!Files.isRegularFile(packPath)) {
            throw new IOException("missing layout pack file: " + packPath);
        }
        JsonObject pack = JsonParser.parseString(Files.readString(packPath)).getAsJsonObject();
        layoutPacks.put(cacheKey, pack);
        return pack;
    }
}
