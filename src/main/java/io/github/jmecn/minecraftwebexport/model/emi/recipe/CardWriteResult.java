package io.github.jmecn.minecraftwebexport.model.emi.recipe;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public record CardWriteResult(
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
        Map<String, ItemStack> iconVariants,
        Map<String, JsonObject> layoutsByRecipeId) {
}
