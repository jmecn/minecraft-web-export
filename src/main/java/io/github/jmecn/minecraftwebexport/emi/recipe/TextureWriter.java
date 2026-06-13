package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.TextureWriteResult;
import io.github.jmecn.minecraftwebexport.model.recipe.TextureManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class TextureWriter {

    private TextureWriter() {
    }


    public static TextureWriteResult export(Path outputDir, Minecraft client, Set<String> textureIds) throws IOException {
        Path texRoot = EmiPaths.resolve(outputDir, Constants.TEXTURES_DIR);
        if (Files.exists(texRoot)) {
            Files.walk(texRoot)
                    .sorted(Comparator.reverseOrder())
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

        ResourceManager resourceManager = client.getResourceManager();
        for (String idString : all) {
            ResourceLocation id = ResourceLocation.parse(idString);
            String relative = textureRelativePath(id);
            Path out = texRoot.resolve(relative);
            Files.createDirectories(out.getParent());

            Optional<Resource> resourceOpt = resourceManager.getResource(id);
            if (resourceOpt.isEmpty()) {
                missing++;
                MweMod.LOGGER.debug("{} missing {}", Log.RECIPE_TEXTURES, idString);
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
                MweMod.LOGGER.debug("{} failed {}: {}", Log.RECIPE_TEXTURES, idString, e);
            }
        }

        JsonIO.write(texRoot.resolve(Constants.TEXTURE_MANIFEST_FILE), TextureManifest.of(manifest));

        MweMod.LOGGER.info(
                "{} {}/{} written ({} bytes), {} missing",
                Log.RECIPE_TEXTURES,
                written,
                all.size(),
                bytes,
                missing);
        return new TextureWriteResult(all.size(), written, missing, bytes);
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
