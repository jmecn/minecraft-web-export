package io.github.jmecn.minecraftwebexport.pipeline.strategy;

import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipeRefsCollector;
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

public final class FullExportStrategy implements ExportStrategy {

    @Override
    public ExportContext planScope(Minecraft client, Seeds seeds) {
        Set<String> recipeIds = Planner.collectExportableRecipeIds(client);
        MinecraftServer server = client.getSingleplayerServer();

        Set<String> categoryIds = new TreeSet<>();
        Set<String> itemIds = new TreeSet<>();
        Set<String> fluidIds = new TreeSet<>();
        Set<String> tagIds = new TreeSet<>();
        Set<String> blockIds = new TreeSet<>();

        for (String recipeId : recipeIds) {
            EmiRecipe recipe = Resolver.resolve(recipeId);
            if (recipe == null) {
                continue;
            }
            RecipeRefsCollector.collectFromRecipe(recipe, itemIds, fluidIds, tagIds, categoryIds);
        }

        if (server != null) {
            TagExpansion expansion = expandItemTagFixedPoint(server, itemIds, fluidIds, blockIds, tagIds);
            itemIds.addAll(expansion.items());
            fluidIds.addAll(expansion.fluids());
            blockIds.addAll(expansion.blocks());
            tagIds.addAll(expansion.tags());
            applyVisibilityFilter(server, itemIds, fluidIds);
        }

        MweMod.LOGGER.info(
                "[export] scope full: recipes={}, items={}, fluids={}, tags={}, categories={}",
                recipeIds.size(),
                itemIds.size(),
                fluidIds.size(),
                tagIds.size(),
                categoryIds.size());

        return ExportContext.builder(Mode.FULL)
                .recipeIds(recipeIds)
                .categoryIds(categoryIds)
                .itemIds(itemIds)
                .fluidIds(fluidIds)
                .blockIds(blockIds)
                .tagIds(tagIds)
                .build();
    }

    static TagExpansion expandItemTagFixedPoint(
            MinecraftServer server,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> blockIds,
            Set<String> tagIds) {
        Set<String> items = new TreeSet<>(itemIds);
        Set<String> fluids = new TreeSet<>(fluidIds);
        Set<String> blocks = new TreeSet<>(blockIds);
        Set<String> tags = new TreeSet<>(tagIds);

        while (true) {
            Set<String> nextTags = new TreeSet<>(tags);
            nextTags.addAll(ItemIndexExporter.collectRegistryTagIds(server, items));

            Set<String> nextItems = new TreeSet<>(items);
            Set<String> nextFluids = new TreeSet<>(fluids);
            Set<String> nextBlocks = new TreeSet<>(blocks);
            if (!nextTags.isEmpty()) {
                TagExpansion expansion = ClosureExpander.expand(server, nextTags);
                nextItems.addAll(expansion.items());
                nextFluids.addAll(expansion.fluids());
                nextBlocks.addAll(expansion.blocks());
                nextTags.addAll(expansion.tags());
            }

            if (nextTags.equals(tags)
                    && nextItems.equals(items)
                    && nextFluids.equals(fluids)
                    && nextBlocks.equals(blocks)) {
                itemIds.clear();
                itemIds.addAll(nextItems);
                fluidIds.clear();
                fluidIds.addAll(nextFluids);
                blockIds.clear();
                blockIds.addAll(nextBlocks);
                tagIds.clear();
                tagIds.addAll(nextTags);
                return new TagExpansion(Set.copyOf(nextItems), Set.copyOf(nextBlocks), Set.copyOf(nextFluids), Set.copyOf(nextTags));
            }
            tags = nextTags;
            items = nextItems;
            fluids = nextFluids;
            blocks = nextBlocks;
        }
    }

    private static void applyVisibilityFilter(
            MinecraftServer server, Set<String> itemIds, Set<String> fluidIds) {
        itemIds.removeIf(id -> !Visibility.shouldExportRegistryId(server, id));
        fluidIds.removeIf(id -> !Visibility.shouldExportRegistryId(server, id));
    }
}
