package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiDrawContext;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import io.github.jmecn.minecraftwebexport.export.module.ExportMode;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** v2 export: {@code recipes/<namespace>/<pathSafe>.{png,json}} per recipe. */
public final class EmiRecipeCardExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiRecipeCardExporter.class);
    private static final Gson GSON = ExportGson.GSON;
    private static final String LOG_STRIDE_PROPERTY = "minecraftWebExport.recipeCardLogStride";

    private EmiRecipeCardExporter() {
    }

    public record Result(
            int requested,
            int written,
            int missing,
            int failures,
            long pngBytes,
            long metaBytes,
            int imageScale,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, net.minecraft.world.item.ItemStack> iconVariants,
            Map<String, JsonObject> layoutsByRecipeId) {
    }

    public static boolean isEnabled() {
        return EmiRecipeLayoutExporter.isEnabled();
    }

    public static int imageScale() {
        return EmiRecipeLayoutExporter.layoutScale();
    }

    public static Result export(
            Path outputDir,
            Minecraft client,
            Set<String> recipeIds,
            LangUsedKeysCollector langKeys) throws IOException {
        Set<String> textureIds = new TreeSet<>();
        Set<String> referencedItems = new TreeSet<>();
        Set<String> referencedFluids = new TreeSet<>();
        Set<String> referencedTags = new TreeSet<>();
        Map<String, net.minecraft.world.item.ItemStack> iconVariants = new LinkedHashMap<>();
        Map<String, JsonObject> layoutsByRecipeId = new LinkedHashMap<>();

        int written = 0;
        int missing = 0;
        int skippedVisibility = 0;
        int failures = 0;
        MinecraftServer server = client.getSingleplayerServer();
        long pngBytes = 0;
        long metaBytes = 0;
        int scale = imageScale();
        int total = recipeIds.size();
        int logStride = ExportProgressLog.stride(total, LOG_STRIDE_PROPERTY, 20, 200);
        int progress = 0;

        for (String recipeId : recipeIds) {
            progress++;
            EmiRecipe recipe = EmiRecipeResolver.resolve(recipeId);
            if (recipe == null) {
                missing++;
                logProgress(progress, total, written, missing, failures, logStride);
                continue;
            }
            if (ExportMode.current() != ExportMode.SCOPED
                    && !EmiExportVisibility.shouldExportRecipe(recipe, server)) {
                skippedVisibility++;
                logProgress(progress, total, written, missing, failures, logStride);
                continue;
            }
            try {
                JsonObject layout = EmiRecipeLayoutExporter.buildLayoutInMemory(
                        client,
                        recipe,
                        recipeId,
                        textureIds,
                        referencedItems,
                        referencedFluids,
                        referencedTags,
                        iconVariants);
                layoutsByRecipeId.put(recipeId, layout);

                JsonObject meta = RecipeMetaBaker.bake(layout);
                if (langKeys != null) {
                    langKeys.collectMeta(meta);
                }
                String metaJson = GSON.toJson(meta);
                metaBytes += metaJson.length();

                Path metaFile = RecipeCardPaths.metaPath(outputDir, recipeId);
                Path pngFile = RecipeCardPaths.pngPath(outputDir, recipeId);
                Files.createDirectories(metaFile.getParent());
                Files.writeString(metaFile, metaJson);

                pngBytes += renderRecipePng(client, recipe, scale, pngFile);
                written++;
                logProgress(progress, total, written, missing, failures, logStride);
            } catch (Exception e) {
                failures++;
                ExportLog.detailFailure(
                        LOGGER,
                        failures,
                        "{} card failed for {}: {}",
                        ExportLog.EMI,
                        recipeId,
                        e);
            }
        }

        LOGGER.info(
                "{} cards done: {}/{} written ({} png bytes, {} meta bytes), {} missing, {} skipped (visibility), {} failed",
                ExportLog.EMI,
                written,
                total,
                pngBytes,
                metaBytes,
                missing,
                skippedVisibility,
                failures);

        return new Result(
                total,
                written,
                missing,
                failures,
                pngBytes,
                metaBytes,
                scale,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants,
                layoutsByRecipeId);
    }

    private static long renderRecipePng(Minecraft client, EmiRecipe recipe, int scale, Path out)
            throws IOException {
        int w = Math.max(1, recipe.getDisplayWidth());
        int h = Math.max(1, recipe.getDisplayHeight());
        int margin = RecipeCardPaths.recipeMargin();
        int logicalW = w + margin;
        int logicalH = h + margin;
        int pixelW = logicalW * scale;
        int pixelH = logicalH * scale;

        try (OffScreenRenderer off = new OffScreenRenderer(pixelW, pixelH)) {
            GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
            off.captureAsPng(() -> off.runWithEmiRecipeMatrices(logicalW, logicalH, () -> {
                EmiRenderHelper.renderRecipe(recipe, EmiDrawContext.wrap(graphics), 0, 0, false, -1);
                graphics.flush();
            }), out);
        }
        return Files.size(out);
    }

    private static void logProgress(
            int progress,
            int total,
            int written,
            int missing,
            int failures,
            int logStride) {
        if (ExportProgressLog.shouldLog(progress, total, logStride)) {
            int pct = ExportProgressLog.percent(progress, total);
            LOGGER.info(
                    "{} cards {}% {}/{} - {} ok, {} missing, {} fail",
                    ExportLog.EMI,
                    pct,
                    progress,
                    total,
                    written,
                    missing,
                    failures);
        }
    }
}
