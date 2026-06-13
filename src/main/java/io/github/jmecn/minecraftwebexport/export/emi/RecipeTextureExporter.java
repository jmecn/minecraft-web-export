package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class RecipeTextureExporter {
    private static final Gson GSON = ExportGson.GSON;

    private RecipeTextureExporter() {
    }

    public record Result(int requested, int written, int missing, long pngBytes) {
    }

    public static Result export(Path outputDir, Minecraft client, Set<String> textureIds) throws IOException {
        Path texRoot = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.TEXTURES_DIR);
        if (Files.exists(texRoot)) {
            Files.walk(texRoot)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
        Files.createDirectories(texRoot);

        Set<String> all = new TreeSet<>(textureIds);
        all.add("emi:textures/gui/widgets.png");
        all.add("emi:textures/gui/background.png");

        Map<String, String> manifest = new TreeMap<>();
        int written = 0;
        int missing = 0;
        long bytes = 0;

        var resourceManager = client.getResourceManager();
        for (String idString : all) {
            ResourceLocation id = ResourceLocation.parse(idString);
            String relative = textureRelativePath(id);
            Path out = texRoot.resolve(relative);
            Files.createDirectories(out.getParent());

            var resourceOpt = resourceManager.getResource(id);
            if (resourceOpt.isEmpty()) {
                missing++;
                MinecraftWebExportMod.LOGGER.debug("{} missing {}", ExportLog.RECIPE_TEXTURES, idString);
                continue;
            }
            try {
                Resource resource = resourceOpt.get();
                try (InputStream input = resource.open()) {
                    NativeImage image = NativeImage.read(input);
                    try {
                        image.writeToFile(out);
                        bytes += Files.size(out);
                        manifest.put(idString, relative.replace('\\', '/'));
                        written++;
                    } finally {
                        image.close();
                    }
                }
            } catch (Exception e) {
                missing++;
                MinecraftWebExportMod.LOGGER.debug("{} failed {}: {}", ExportLog.RECIPE_TEXTURES, idString, e);
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("textures", manifest);
        Files.writeString(texRoot.resolve(RecipeLayoutPaths.TEXTURE_MANIFEST_FILE), GSON.toJson(root));

        MinecraftWebExportMod.LOGGER.info(
                "{} {}/{} written ({} bytes), {} missing",
                ExportLog.RECIPE_TEXTURES,
                written,
                all.size(),
                bytes,
                missing);
        return new Result(all.size(), written, missing, bytes);
    }

    static String textureRelativePath(ResourceLocation id) {
        return textureRelativePath(id.toString());
    }

    static String textureRelativePath(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) {
            return id;
        }
        return id.substring(0, colon) + "/" + id.substring(colon + 1);
    }
}
