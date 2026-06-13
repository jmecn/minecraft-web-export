package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonObject;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiDrawContext;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRendererPool;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.RecipeWriteResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.MinecraftServer;

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
            Path outputDir, Minecraft client, ExportContext context, ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        if (!isEnabled()) {
            MweMod.LOGGER.info("{} recipe export disabled by configuration", Log.EMI);
            return emptyResult(context.recipeIds().size());
        }

        int written = 0;
        int missing = 0;
        int skippedVisibility = 0;
        int failures = 0;
        long pngBytes = 0;
        long metaBytes = 0;
        int scale = imageScale();
        int total = context.recipeIds().size();
        int logStride = ProgressLog.stride(total, MweConfig.recipeCardLogStride(), 20, 200);
        int progress = 0;
        MinecraftServer server = client.getSingleplayerServer();
        Set<String> textureIds = new TreeSet<>();

        try (OffScreenRendererPool rendererPool = new OffScreenRendererPool()) {
            for (String recipeId : context.recipeIds()) {
                progress++;
                EmiRecipe recipe = Resolver.resolve(recipeId);
                if (recipe == null) {
                    missing++;
                    logProgress(progress, total, written, missing, failures, logStride);
                    continue;
                }
                if (context.mode() != Mode.SCOPED
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
                            context.referencedItems(),
                            context.referencedFluids(),
                            context.referencedTags(),
                            context.iconVariants());

                    RecipeMeta meta = MetaBaker.bake(layout);
                    Path metaFile = RecipePaths.metaPath(outputDir, recipeId);
                    Path pngFile = RecipePaths.pngPath(outputDir, recipeId);
                    byte[] metaUtf8 = JsonIO.toUtf8Bytes(meta);
                    writes.submitBytes(metaFile, metaUtf8);
                    metaBytes += metaUtf8.length;

                    pngBytes += renderRecipePng(client, recipe, scale, pngFile, rendererPool, writes);
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

        return new RecipeWriteResult(total, written, missing, failures, pngBytes, metaBytes, scale);
    }

    private static RecipeWriteResult emptyResult(int requested) {
        return new RecipeWriteResult(requested, 0, 0, 0, 0, 0, imageScale());
    }

    private static int renderRecipePng(
            Minecraft client,
            EmiRecipe recipe,
            int scale,
            Path out,
            OffScreenRendererPool rendererPool,
            ExportWriteQueue writes) {
        int w = Math.max(1, recipe.getDisplayWidth());
        int h = Math.max(1, recipe.getDisplayHeight());
        int margin = RecipePaths.recipeMargin();
        int logicalW = w + margin;
        int logicalH = h + margin;
        int pixelW = logicalW * scale;
        int pixelH = logicalH * scale;

        OffScreenRenderer off = rendererPool.borrow(pixelW, pixelH);
        GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
        byte[] png = off.captureAsPng(() -> off.runWithEmiRecipeMatrices(logicalW, logicalH, () -> {
            EmiRenderHelper.renderRecipe(recipe, EmiDrawContext.wrap(graphics), 0, 0, false, -1);
            graphics.flush();
        }));
        writes.submitBytes(out, png);
        return png.length;
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
