package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonObject;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiDrawContext;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRendererPool;
import io.github.jmecn.minecraftwebexport.emi.lang.UsedKeysCollector;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.RecipeWriteResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

public final class RecipeWriter {

    private RecipeWriter() {
    }

    public static boolean isEnabled() {
        return LayoutBuilder.isEnabled();
    }

    public static int imageScale() {
        return LayoutBuilder.layoutScale();
    }

    public static RecipeWriteResult export(
            Path outputDir,
            Minecraft client,
            Set<String> recipeIds,
            UsedKeysCollector langKeys) throws IOException {
        Set<String> textureIds = new TreeSet<>();
        Set<String> referencedItems = new TreeSet<>();
        Set<String> referencedFluids = new TreeSet<>();
        Set<String> referencedTags = new TreeSet<>();
        Map<String, ItemStack> iconVariants = new LinkedHashMap<>();
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
        int logStride = ProgressLog.stride(total, Constants.PROP_RECIPE_LOG_STRIDE, 20, 200);
        int progress = 0;

        try (OffScreenRendererPool rendererPool = new OffScreenRendererPool()) {
            for (String recipeId : recipeIds) {
                progress++;
                EmiRecipe recipe = Resolver.resolve(recipeId);
                if (recipe == null) {
                    missing++;
                    logProgress(progress, total, written, missing, failures, logStride);
                    continue;
                }
                if (Mode.current() != Mode.SCOPED
                        && !Visibility.shouldExportRecipe(recipe, server)) {
                    skippedVisibility++;
                    logProgress(progress, total, written, missing, failures, logStride);
                    continue;
                }
                try {
                    JsonObject layout = LayoutBuilder.buildLayoutInMemory(
                            client,
                            recipe,
                            recipeId,
                            textureIds,
                            referencedItems,
                            referencedFluids,
                            referencedTags,
                            iconVariants);
                    layoutsByRecipeId.put(recipeId, layout);

                    RecipeMeta meta = MetaBaker.bake(layout);
                    if (langKeys != null) {
                        langKeys.collectMeta(meta);
                    }
                    Path metaFile = RecipePaths.metaPath(outputDir, recipeId);
                    Path pngFile = RecipePaths.pngPath(outputDir, recipeId);
                    JsonIO.write(metaFile, meta);
                    metaBytes += JsonIO.toUtf8Bytes(meta).length;

                    pngBytes += renderRecipePng(client, recipe, scale, pngFile, rendererPool);
                    written++;
                    logProgress(progress, total, written, missing, failures, logStride);
                } catch (Exception e) {
                    failures++;
                    Log.detailFailure(failures,
                            "{} recipe failed for {}: {}",
                            Log.EMI,
                            recipeId,
                            e);
                }
            }
        }

        MweMod.LOGGER.info(
                "{} recipes done: {}/{} written ({} png bytes, {} meta bytes), {} missing, {} skipped (visibility), {} failed",
                Log.EMI,
                written,
                total,
                pngBytes,
                metaBytes,
                missing,
                skippedVisibility,
                failures);

        return new RecipeWriteResult(
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

    private static long renderRecipePng(
            Minecraft client,
            EmiRecipe recipe,
            int scale,
            Path out,
            OffScreenRendererPool rendererPool) throws IOException {
        int w = Math.max(1, recipe.getDisplayWidth());
        int h = Math.max(1, recipe.getDisplayHeight());
        int margin = RecipePaths.recipeMargin();
        int logicalW = w + margin;
        int logicalH = h + margin;
        int pixelW = logicalW * scale;
        int pixelH = logicalH * scale;

        OffScreenRenderer off = rendererPool.borrow(pixelW, pixelH);
        GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
        off.captureAsPng(() -> off.runWithEmiRecipeMatrices(logicalW, logicalH, () -> {
            EmiRenderHelper.renderRecipe(recipe, EmiDrawContext.wrap(graphics), 0, 0, false, -1);
            graphics.flush();
        }), out);
        return Files.size(out);
    }

    private static void logProgress(
            int progress,
            int total,
            int written,
            int missing,
            int failures,
            int logStride) {
        if (ProgressLog.shouldLog(progress, total, logStride)) {
            int pct = ProgressLog.percent(progress, total);
            MweMod.LOGGER.info(
                    "{} recipes {}% {}/{} - {} ok, {} missing, {} fail",
                    Log.EMI,
                    pct,
                    progress,
                    total,
                    written,
                    missing,
                    failures);
        }
    }
}
