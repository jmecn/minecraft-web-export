package io.github.jmecn.minecraftwebexport.model.emi.recipe;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

public record RecipeWriteResult(
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
