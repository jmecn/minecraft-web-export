package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

public record WidgetSerializeContext(
        Set<String> referencedItems,
        Set<String> referencedFluids,
        Set<String> referencedTags,
        Map<String, ItemStack> iconVariants) {
}
