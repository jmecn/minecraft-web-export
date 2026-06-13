package io.github.jmecn.minecraftwebexport.pipeline;

import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.UsedKeysCollector;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.recipe.LayoutBuilder;
import io.github.jmecn.minecraftwebexport.emi.recipe.MetaBaker;
import io.github.jmecn.minecraftwebexport.emi.recipe.Resolver;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;

public final class RelationPlanner {

    private RelationPlanner() {
    }

    public static void buildRelations(Minecraft client, ExportContext context) {
        if (!context.hasRecipes()) {
            return;
        }

        UsedKeysCollector langCollector = new UsedKeysCollector();
        int total = context.recipeIds().size();
        int progress = 0;
        int logStride = ProgressLog.stride(total, MweConfig.layoutLogStride(), 20, 200);

        for (String recipeId : context.recipeIds()) {
            progress++;
            EmiRecipe recipe = Resolver.resolve(recipeId);
            if (recipe == null) {
                logProgress(progress, total, logStride);
                continue;
            }
            if (context.mode() == Mode.FULL
                    && !Visibility.shouldExportRecipe(recipe, client.getSingleplayerServer())) {
                logProgress(progress, total, logStride);
                continue;
            }

            JsonObject layout = LayoutBuilder.buildLayoutInMemory(
                    client,
                    recipe,
                    recipeId,
                    new TreeSet<>(),
                    context.referencedItems(),
                    context.referencedFluids(),
                    context.referencedTags(),
                    context.iconVariants());

            RecipeMeta meta = MetaBaker.bake(layout);
            langCollector.collectMeta(meta);
            ItemIndexExporter.accumulateRecipeRefsFromLayout(
                    recipeId,
                    layout,
                    context.inputs(),
                    context.outputs(),
                    context.fluidRegistryIds());
            logProgress(progress, total, logStride);
        }

        context.recipeLangKeys().addAll(langCollector.snapshot());
        MweMod.LOGGER.info(
                "{} relation: {} input items, {} output items, {} fluids, {} tags, {} lang keys",
                Log.EMI,
                context.inputs().size(),
                context.outputs().size(),
                context.fluidRegistryIds().size(),
                context.referencedTags().size(),
                context.recipeLangKeys().size());
    }

    private static void logProgress(int progress, int total, int logStride) {
        if (ProgressLog.shouldLog(progress, total, logStride)) {
            MweMod.LOGGER.info(
                    "{} relation {}% {}/{}",
                    Log.EMI,
                    ProgressLog.percent(progress, total),
                    progress,
                    total);
        }
    }
}
