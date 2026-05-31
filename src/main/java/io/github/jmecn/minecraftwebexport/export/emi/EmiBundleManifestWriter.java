package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import io.github.jmecn.minecraftwebexport.export.ExportGson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmiBundleManifestWriter {

    private static final Gson GSON = ExportGson.GSON;

    private EmiBundleManifestWriter() {
    }

    public static void write(
            Path outputDir,
            List<String> languages,
            int imageScale,
            int recipeCount) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", RecipeCardPaths.BUNDLE_SCHEMA);
        root.put("imageScale", imageScale);
        root.put("languages", languages);
        root.put("recipeCount", recipeCount);
        root.put("recipeImageFormat", "png");
        root.put("missingIconId", IconPlaceholderRenderer.REGISTRY_ID);

        Path out = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.BUNDLE_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root));
    }
}
