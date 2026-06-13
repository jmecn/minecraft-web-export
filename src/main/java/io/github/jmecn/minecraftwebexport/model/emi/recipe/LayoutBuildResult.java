package io.github.jmecn.minecraftwebexport.model.emi.recipe;

import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public record LayoutBuildResult(
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
        TextureWriteResult textures,
        BundleMods mods) {
}
