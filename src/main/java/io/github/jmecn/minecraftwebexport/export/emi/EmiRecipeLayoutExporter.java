package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class EmiRecipeLayoutExporter {

    private static final Logger LOGGER = Logger.getLogger(EmiRecipeLayoutExporter.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int PANEL_MARGIN = 4;
    /** Target ~30 progress lines for large exports unless {@code minecraftWebExport.layoutLogStride} is set. */
    private static final int TARGET_PROGRESS_LOGS = 30;

    private EmiRecipeLayoutExporter() {
    }

    static int progressLogStride(int total) {
        String prop = System.getProperty("minecraftWebExport.layoutLogStride", "").trim();
        if (!prop.isEmpty()) {
            return Math.max(1, Integer.parseInt(prop));
        }
        if (total <= 0) {
            return 1;
        }
        if (total <= 200) {
            return 20;
        }
        if (total <= 2_000) {
            return 200;
        }
        return Math.max(2_000, (total + TARGET_PROGRESS_LOGS - 1) / TARGET_PROGRESS_LOGS);
    }

    private static boolean shouldLogProgress(int progress, int total, int stride) {
        return progress == total || progress % stride == 0;
    }

    public record Result(
            int requested,
            int written,
            int missing,
            int failures,
            int chromeLayers,
            int chromeDeduped,
            int uniqueChromeFiles,
            long jsonBytes,
            long chromeBytes,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants,
            RecipeTextureExporter.Result textures) {
    }

    public static boolean isEnabled() {
        if (Boolean.getBoolean("minecraftWebExport.skipEmiLayoutExport")) {
            return false;
        }
        String prop = System.getProperty("minecraftWebExport.exportEmiLayout");
        if (prop != null) {
            return !"false".equalsIgnoreCase(prop);
        }
        return true;
    }

    public static int layoutScale() {
        return Math.max(1, Integer.getInteger("minecraftWebExport.recipeLayoutScale", 2));
    }

    public static Result export(Path outputDir, Minecraft client, Set<String> recipeIds) throws IOException {
        Path layoutsRoot = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.LAYOUTS_DIR);
        Path chromeRoot = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.CHROME_DIR);
        Files.createDirectories(layoutsRoot);
        Files.createDirectories(chromeRoot);

        Set<String> textureIds = new TreeSet<>();
        Set<String> referencedItems = new TreeSet<>();
        Set<String> referencedFluids = new TreeSet<>();
        Set<String> referencedTags = new TreeSet<>();
        Map<String, ItemStack> iconVariants = new LinkedHashMap<>();
        Map<String, String> chromeHashToRelative = new ConcurrentHashMap<>();
        List<String> indexRecipeIds = new ArrayList<>();
        int written = 0;
        int missing = 0;
        int failures = 0;
        int chromeLayers = 0;
        int chromeDeduped = 0;
        long jsonBytes = 0;
        int total = recipeIds.size();
        int logStride = progressLogStride(total);
        int progress = 0;

        for (String recipeId : recipeIds) {
            progress++;
            EmiRecipe recipe = EmiRecipeResolver.resolve(recipeId);
            if (recipe == null) {
                missing++;
                if (shouldLogProgress(progress, total, logStride)) {
                    LOGGER.warning("[emi-layout] " + progress + "/" + total + " - " + missing + " missing so far");
                }
                continue;
            }
            try {
                int[] chromeWritten = {0};
                int[] chromeDedupedCount = {0};
                JsonObject layout = buildLayout(
                        client,
                        recipe,
                        recipeId,
                        textureIds,
                        referencedItems,
                        referencedFluids,
                        referencedTags,
                        iconVariants,
                        chromeRoot,
                        chromeHashToRelative,
                        chromeWritten,
                        chromeDedupedCount);
                chromeLayers += chromeWritten[0];
                chromeDeduped += chromeDedupedCount[0];

                String fileName = RecipeLayoutPaths.relativeLayoutJson(recipeId);
                Path out = layoutsRoot.resolve(fileName);
                String json = GSON.toJson(layout);
                Files.writeString(out, json);
                jsonBytes += json.length();

                indexRecipeIds.add(recipeId);
                written++;
                if (shouldLogProgress(progress, total, logStride)) {
                    int pct = total > 0 ? (progress * 100 / total) : 100;
                    LOGGER.info("[emi-layout] " + pct + "% " + progress + "/" + total + " - "
                            + written + " ok, " + missing + " missing, " + failures + " fail");
                }
            } catch (Exception e) {
                failures++;
                LOGGER.warning("[emi-layout] failed for " + recipeId + ": " + e);
            }
        }

        RecipeLayoutIndexWriter.write(outputDir, layoutScale(), indexRecipeIds);
        RecipeTextureExporter.Result textures = RecipeTextureExporter.export(outputDir, client, textureIds);

        long chromeBytes = dirSize(chromeRoot);
        LOGGER.info("[emi-layout] done: " + written + "/" + total + " layouts (" + jsonBytes + " json bytes), "
                + missing + " missing, " + failures + " failed, chrome layers " + chromeLayers
                + " (" + chromeDeduped + " deduped), " + chromeHashToRelative.size()
                + " unique files, " + chromeBytes + " chrome bytes");
        return new Result(
                total,
                written,
                missing,
                failures,
                chromeLayers,
                chromeDeduped,
                chromeHashToRelative.size(),
                jsonBytes,
                chromeBytes,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants,
                textures);
    }

    private static JsonObject buildLayout(
            Minecraft client,
            EmiRecipe recipe,
            String recipeId,
            Set<String> textureIds,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants,
            Path chromeRoot,
            Map<String, String> chromeHashToRelative,
            int[] chromeWritten,
            int[] chromeDedupedCount) {
        int displayWidth = Math.max(1, recipe.getDisplayWidth());
        int displayHeight = Math.max(1, recipe.getDisplayHeight());

        List<Widget> widgets = new ArrayList<>();
        WidgetHolder holder = new WidgetHolder() {
            @Override
            public int getWidth() {
                return displayWidth;
            }

            @Override
            public int getHeight() {
                return displayHeight;
            }

            @Override
            public <T extends Widget> T add(T widget) {
                widgets.add(widget);
                return widget;
            }
        };
        recipe.addWidgets(holder);

        JsonArray widgetArray = new JsonArray();
        EmiWidgetSerializer.Context context = new EmiWidgetSerializer.Context(
                client,
                chromeRoot,
                chromeHashToRelative,
                chromeWritten,
                chromeDedupedCount,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants);
        EmiWidgetSerializer.serializeWidgets(recipe, widgets, textureIds, context, widgetArray::add);

        JsonObject panel = new JsonObject();
        panel.addProperty("width", displayWidth);
        panel.addProperty("height", displayHeight);
        panel.addProperty("margin", PANEL_MARGIN);
        panel.addProperty("frameWidth", displayWidth + PANEL_MARGIN * 2);
        panel.addProperty("frameHeight", displayHeight + PANEL_MARGIN * 2);

        JsonObject root = new JsonObject();
        root.addProperty("schema", RecipeLayoutPaths.SCHEMA_VERSION);
        root.addProperty("id", recipeId);
        root.addProperty("scale", layoutScale());
        root.add("panel", panel);

        ResourceLocation emiId = recipe.getId();
        if (emiId != null) {
            root.addProperty("emiId", emiId.toString());
        }
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            root.addProperty("category", recipe.getCategory().getId().toString());
        }

        root.add("widgets", widgetArray);
        return root;
    }

    private static long dirSize(Path root) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try (var walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
