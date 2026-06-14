package io.github.jmecn.minecraftwebexport.pipeline;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.recipe.LayoutBuilder;
import io.github.jmecn.minecraftwebexport.emi.recipe.Resolver;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import net.minecraft.client.Minecraft;

public final class RelationPlanner {

    private RelationPlanner() {
    }

    public static void buildRelations(Minecraft client, ExportContext context) {
        if (!context.hasRecipes()) {
            return;
        }

        LangKeysCollector langCollector = new LangKeysCollector();
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

            RecipeMeta meta = LayoutBuilder.bake(layout);
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

    static final class LangKeysCollector {

        private final Set<String> keys = new TreeSet<>();

        int size() {
            return keys.size();
        }

        Set<String> snapshot() {
            return Set.copyOf(keys);
        }

        void collectMeta(RecipeMeta meta) {
            if (meta == null || meta.widgets() == null) {
                return;
            }
            for (RecipeWidget widget : meta.widgets()) {
                collectInteraction(widget.interaction());
            }
        }

        private void collectInteraction(WidgetInteraction interaction) {
            if (interaction == null) {
                return;
            }
            String kind = interaction.kind();
            if ("item".equals(kind)) {
                if (interaction.id() != null) {
                    addRegistryItem(interaction.id());
                }
                if (interaction.nbt() != null) {
                    collectFluidFromNbt(interaction.nbt());
                }
                return;
            }
            if ("fluid".equals(kind)) {
                if (interaction.id() != null) {
                    addRegistryFluid(interaction.id());
                }
                return;
            }
            if ("tag".equals(kind)) {
                if (interaction.tag() != null) {
                    addTag(interaction.tag());
                }
                if (interaction.displayId() != null) {
                    addRegistryItem(interaction.displayId());
                }
                return;
            }
            if ("list".equals(kind) && interaction.entries() != null) {
                for (WidgetInteraction entry : interaction.entries()) {
                    collectInteraction(entry);
                }
            }
        }

        private void collectFluidFromNbt(JsonElement nbt) {
            String raw = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
            Matcher matcher = Constants.FLUID_NBT_NAME_PATTERN.matcher(raw);
            if (matcher.find()) {
                addRegistryFluid(matcher.group(1));
            }
        }

        private void addRegistryItem(String registryId) {
            ClosureKeys.addForItem(keys, registryId);
        }

        private void addRegistryFluid(String registryId) {
            ClosureKeys.addForFluid(keys, registryId);
        }

        private void addTag(String tagId) {
            if (tagId == null || tagId.isEmpty()) {
                return;
            }
            String dotted = tagId.replace('/', '.').replace(':', '.');
            keys.add("tag.item." + dotted);
            keys.add("tag.block." + dotted);
            keys.add("tag.fluid." + dotted);
        }
    }
}
