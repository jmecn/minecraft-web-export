package io.github.jmecn.minecraftwebexport.pipeline.strategy;

import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.recipe.WidgetSerializer;
import io.github.jmecn.minecraftwebexport.emi.recipe.Resolver;
import io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagExpansion;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

public final class ScopedExportStrategy implements ExportStrategy {

    @Override
    public ExportContext planScope(Minecraft client, Seeds seeds) {
        Set<String> availableRecipes = Planner.collectExportableRecipeIds(client);
        Set<String> recipeIds = Planner.resolveRecipeIds(Mode.SCOPED, seeds.recipeIds(), availableRecipes);

        Set<String> categoryIds = new TreeSet<>();
        Set<String> itemIds = new TreeSet<>(seeds.itemIds());
        Set<String> fluidIds = new TreeSet<>(seeds.fluidIds());
        Set<String> blockIds = new TreeSet<>(seeds.blockIds());
        Set<String> tagIds = new TreeSet<>(seeds.tagIds());

        for (String recipeId : recipeIds) {
            EmiRecipe recipe = Resolver.resolve(recipeId);
            if (recipe == null) {
                continue;
            }
            WidgetSerializer.collectFromRecipe(recipe, itemIds, fluidIds, tagIds, categoryIds);
        }

        MinecraftServer server = client.getSingleplayerServer();
        if (server != null && !tagIds.isEmpty()) {
            TagExpansion expansion = ClosureExpander.expand(server, tagIds);
            itemIds.addAll(expansion.items());
            fluidIds.addAll(expansion.fluids());
            blockIds.addAll(expansion.blocks());
            tagIds.addAll(expansion.tags());
        } else if (server == null && !seeds.tagIds().isEmpty()) {
            MweMod.LOGGER.warn("[export] tag closure skipped: no integrated server (seedTags={})", seeds.tagIds().size());
        }

        return ExportContext.builder(Mode.SCOPED)
                .recipeIds(recipeIds)
                .categoryIds(categoryIds)
                .itemIds(itemIds)
                .fluidIds(fluidIds)
                .blockIds(blockIds)
                .tagIds(tagIds)
                .seedLangKeys(seeds.langKeys())
                .build();
    }
}
