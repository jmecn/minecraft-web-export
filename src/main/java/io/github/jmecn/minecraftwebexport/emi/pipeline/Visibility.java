package io.github.jmecn.minecraftwebexport.emi.pipeline;

import com.google.common.collect.Iterables;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiHidden;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
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

    public static boolean isEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_EMI_VISIBILITY_FILTER);
    }

    public static java.util.Set<String> filterExportableRecipeIds(MinecraftServer server, Iterable<EmiRecipe> recipes) {
        java.util.Set<String> ids = new java.util.TreeSet<>();
        if (!isEnabled()) {
            for (EmiRecipe recipe : recipes) {
                if (recipe != null && recipe.getId() != null) {
                    ids.add(recipe.getId().toString());
                }
            }
            return java.util.Set.copyOf(ids);
        }
        int skippedDisabled = 0;
        int skippedHiddenTag = 0;
        for (EmiRecipe recipe : recipes) {
            if (recipe == null || recipe.getId() == null) {
                continue;
            }
            Exclusion reason = exclusionReason(recipe, server);
            if (reason == Exclusion.DISABLED_STACK) {
                skippedDisabled++;
            } else if (reason == Exclusion.HIDDEN_TAG) {
                skippedHiddenTag++;
            } else {
                ids.add(recipe.getId().toString());
            }
        }
        MweMod.LOGGER.info(
                "{} recipe visibility: {} exportable, {} skipped ({} emi-disabled ingredient, {} hidden-tag ingredient)",
                Log.EMI,
                ids.size(),
                skippedDisabled + skippedHiddenTag,
                skippedDisabled,
                skippedHiddenTag);
        return java.util.Set.copyOf(ids);
    }

    public static boolean shouldExportRecipe(EmiRecipe recipe, MinecraftServer server) {
        return exclusionReason(recipe, server) == Exclusion.NONE;
    }

    public static boolean shouldExportRegistryId(MinecraftServer server, String registryId) {
        return !isRegistryIdHiddenFromRecipeViewers(server, registryId);
    }

    private enum Exclusion {
        NONE,
        DISABLED_STACK,
        HIDDEN_TAG
    }

    private static Exclusion exclusionReason(EmiRecipe recipe, MinecraftServer server) {
        if (!isEnabled()) {
            return Exclusion.NONE;
        }
        for (EmiIngredient ingredient : Iterables.concat(
                recipe.getInputs(), recipe.getOutputs(), recipe.getCatalysts())) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }
            if (EmiHidden.isDisabled(ingredient)) {
                return Exclusion.DISABLED_STACK;
            }
            if (server != null) {
                for (EmiStack stack : ingredient.getEmiStacks()) {
                    if (isEmiStackHiddenFromRecipeViewers(server, stack)) {
                        return Exclusion.HIDDEN_TAG;
                    }
                }
            }
        }
        return Exclusion.NONE;
    }

    static boolean isRegistryIdHiddenFromRecipeViewers(MinecraftServer server, String registryId) {
        if (!isEnabled() || server == null || registryId == null || registryId.isBlank()) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(RegistryKeys.normalizeRegistryId(registryId));
        if (location == null) {
            return false;
        }
        var access = server.registryAccess();
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

    private static boolean isEmiStackHiddenFromRecipeViewers(MinecraftServer server, EmiStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Object key = stack.getKey();
        if (key instanceof Item) {
            ResourceLocation id = stack.getId();
            if (id == null) {
                return false;
            }
            return isRegistryIdHiddenFromRecipeViewers(server, id.toString());
        }
        if (key instanceof Fluid) {
            ResourceLocation id = stack.getId();
            if (id == null) {
                return false;
            }
            return isRegistryIdHiddenFromRecipeViewers(server, id.toString());
        }
        return false;
    }
}
