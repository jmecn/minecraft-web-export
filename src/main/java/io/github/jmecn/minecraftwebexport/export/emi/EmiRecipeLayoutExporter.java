package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class EmiRecipeLayoutExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiRecipeLayoutExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private static final int PANEL_MARGIN = 4;
    private static final String LAYOUT_LOG_STRIDE_PROPERTY = "minecraftWebExport.layoutLogStride";

    private EmiRecipeLayoutExporter() {
    }

    static int progressLogStride(int total) {
        return ExportProgressLog.stride(total, LAYOUT_LOG_STRIDE_PROPERTY, 20, 200);
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
            RecipeTextureExporter.Result textures,
            RecipeBundleMods mods) {
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
        Path chromeRoot = EmiBundlePaths.resolve(outputDir, RecipeLayoutPaths.CHROME_DIR);
        Files.createDirectories(chromeRoot);

        RecipeRoutePackWriter routePackWriter = new RecipeRoutePackWriter(
                outputDir,
                RecipeRoutePackWriter.defaultPackMaxBytes());

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
                if (ExportProgressLog.shouldLog(progress, total, logStride)) {
                    LOGGER.info(
                            "{} {}/{} progress: {} missing so far",
                            ExportLog.EMI_LAYOUT,
                            progress,
                            total,
                            missing);
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

                String json = GSON.toJson(layout);
                jsonBytes += json.length();
                routePackWriter.addLayout(recipeId, layout);

                indexRecipeIds.add(recipeId);
                written++;
                if (ExportProgressLog.shouldLog(progress, total, logStride)) {
                    int pct = ExportProgressLog.percent(progress, total);
                    LOGGER.info(
                            "{} {}% {}/{} - {} ok, {} missing, {} fail",
                            ExportLog.EMI_LAYOUT,
                            pct,
                            progress,
                            total,
                            written,
                            missing,
                            failures);
                }
            } catch (Exception e) {
                failures++;
                ExportLog.detailFailure(
                        LOGGER,
                        failures,
                        "{} failed for {}: {}",
                        ExportLog.EMI_LAYOUT,
                        recipeId,
                        e);
            }
        }

        RecipeBundleMods mods = routePackWriter.finish();
        RecipeTextureExporter.Result textures = RecipeTextureExporter.export(outputDir, client, textureIds);

        long chromeBytes = dirSize(chromeRoot);
        LOGGER.info(
                "{} done: {}/{} layouts ({} json bytes), {} missing, {} failed, chrome layers {} ({} deduped), {} unique files, {} chrome bytes",
                ExportLog.EMI_LAYOUT,
                written,
                total,
                jsonBytes,
                missing,
                failures,
                chromeLayers,
                chromeDeduped,
                chromeHashToRelative.size(),
                chromeBytes);
        if (failures > ExportLog.DETAIL_FAILURE_LIMIT) {
            LOGGER.warn(
                    "{} {} layout failures (first {} at DEBUG; -D{}=true or enable DEBUG on export.emi)",
                    ExportLog.EMI_LAYOUT,
                    failures,
                    ExportLog.DETAIL_FAILURE_LIMIT,
                    "minecraftWebExport.export.logDetailFailures");
        }
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
                textures,
                mods);
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
