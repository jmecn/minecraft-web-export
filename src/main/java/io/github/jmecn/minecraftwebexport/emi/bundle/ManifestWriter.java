package io.github.jmecn.minecraftwebexport.emi.bundle;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.icon.PlaceholderRenderer;
import io.github.jmecn.minecraftwebexport.emi.recipe.CardPaths;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManifestWriter {

    private static final com.google.gson.Gson GSON = io.github.jmecn.minecraftwebexport.emi.bundle.Gson.GSON;

    private ManifestWriter() {
    }

    public static void write(
            Path outputDir,
            List<String> languages,
            int imageScale,
            int recipeCount) throws IOException {
        write(outputDir, languages, imageScale, recipeCount, null);
    }

    public static void write(
            Path outputDir,
            List<String> languages,
            int imageScale,
            int recipeCount,
            List<String> itemsLangLocales) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", CardPaths.BUNDLE_SCHEMA);
        root.put("imageScale", imageScale);
        root.put("languages", languages);
        root.put("recipeCount", recipeCount);
        root.put("recipeImageFormat", "png");
        root.put("missingIconId", PlaceholderRenderer.REGISTRY_ID);
        if (itemsLangLocales != null && !itemsLangLocales.isEmpty()) {
            root.put("itemsLang", Map.of(
                    "dir", Paths.ITEMS_LANG_DIR,
                    "locales", List.copyOf(itemsLangLocales)));
        }

        Path out = Paths.resolve(outputDir, Paths.BUNDLE_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root));
    }
}
