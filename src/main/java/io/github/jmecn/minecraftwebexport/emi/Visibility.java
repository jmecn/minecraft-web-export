package io.github.jmecn.minecraftwebexport.emi;

import com.google.common.collect.Iterables;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiHidden;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class Visibility {

    public static final ResourceLocation HIDDEN_FROM_RECIPE_VIEWERS_TAG =
            ResourceLocation.fromNamespaceAndPath("c", "hidden_from_recipe_viewers");

    private Visibility() {
    }

    public static Set<String> filterExportableRecipeIds(MinecraftServer server, Iterable<EmiRecipe> recipes) {
        Set<String> ids = new TreeSet<>();
        int skippedDisabled = 0;
        for (EmiRecipe recipe : recipes) {
            if (recipe == null || recipe.getId() == null) {
                continue;
            }
            if (isRecipeDisabledByEmi(recipe)) {
                skippedDisabled++;
            } else {
                ids.add(recipe.getId().toString());
            }
        }
        MweMod.LOGGER.info(
                "{} recipe visibility: {} exportable, {} skipped ({} emi-disabled ingredient)",
                Log.EMI,
                ids.size(),
                skippedDisabled,
                skippedDisabled);
        return Set.copyOf(ids);
    }

    public static boolean shouldExportRecipe(EmiRecipe recipe, MinecraftServer server) {
        return !isRecipeDisabledByEmi(recipe);
    }

    /**
     * Aligns with EMI recipe bake ({@code EmiRecipes}): only {@code EmiHidden.isDisabled} drops a recipe.
     * {@code c:hidden_from_recipe_viewers} hides individual stacks from the item index, not whole recipes
     * that reference broad tags containing a few hidden members.
     */
    private static boolean isRecipeDisabledByEmi(EmiRecipe recipe) {
        for (EmiIngredient ingredient : Iterables.concat(
                recipe.getInputs(), recipe.getOutputs(), recipe.getCatalysts())) {
            if (ingredient != null && !ingredient.isEmpty() && EmiHidden.isDisabled(ingredient)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldExportRegistryId(MinecraftServer server, String registryId) {
        return !isRegistryIdHiddenFromRecipeViewers(server, registryId);
    }

    static boolean isRegistryIdHiddenFromRecipeViewers(MinecraftServer server, String registryId) {
        if (server == null || registryId == null || registryId.isBlank()) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(RegistryKeys.normalizeRegistryId(registryId));
        if (location == null) {
            return false;
        }
        RegistryAccess access = server.registryAccess();
        Registry<Item> items = access.registryOrThrow(Registries.ITEM);
        Registry<Block> blocks = access.registryOrThrow(Registries.BLOCK);
        Registry<Fluid> fluids = access.registryOrThrow(Registries.FLUID);
        TagKey<Item> itemHidden = TagKey.create(Registries.ITEM, HIDDEN_FROM_RECIPE_VIEWERS_TAG);
        TagKey<Block> blockHidden = TagKey.create(Registries.BLOCK, HIDDEN_FROM_RECIPE_VIEWERS_TAG);
        TagKey<Fluid> fluidHidden = TagKey.create(Registries.FLUID, HIDDEN_FROM_RECIPE_VIEWERS_TAG);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, location);
        if (items.containsKey(location)) {
            return items.getHolder(itemKey)
                    .map(holder -> isItemHolderHidden(blocks, holder, itemHidden, blockHidden))
                    .orElse(false);
        }
        ResourceKey<Fluid> fluidKey = ResourceKey.create(Registries.FLUID, location);
        return fluids.getHolder(fluidKey)
                .map(holder -> holder.is(fluidHidden))
                .orElse(false);
    }

    private static boolean isItemHolderHidden(
            Registry<Block> blocks,
            Holder<Item> holder,
            TagKey<Item> itemHidden,
            TagKey<Block> blockHidden) {
        Item item = holder.value();
        if (item instanceof BlockItem blockItem) {
            boolean blockIsHidden = blocks.getResourceKey(blockItem.getBlock())
                    .flatMap(blocks::getHolder)
                    .map(blockHolder -> blockHolder.is(blockHidden))
                    .orElse(false);
            if (blockIsHidden) {
                return true;
            }
        }
        return holder.is(itemHidden);
    }
}
