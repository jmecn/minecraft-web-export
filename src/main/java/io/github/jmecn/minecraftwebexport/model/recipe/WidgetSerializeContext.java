package io.github.jmecn.minecraftwebexport.model.recipe;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public record WidgetSerializeContext(
        Minecraft client,
        Path chromeRoot,
        Map<String, String> chromeHashToRelative,
        int[] chromeWritten,
        int[] chromeDeduped,
        Set<String> referencedItems,
        Set<String> referencedFluids,
        Set<String> referencedTags,
        Map<String, ItemStack> iconVariants) {
}
