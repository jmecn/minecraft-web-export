package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class IconItemOrdering {

    private IconItemOrdering() {
    }

    public static List<ResourceLocation> orderedForPass(
            Set<String> onlyItemIds,
            Predicate<Item> include,
            Map<String, Integer> usageWeights) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR || !include.test(item)) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }
            if (onlyItemIds != null && !onlyItemIds.contains(itemId.toString())) {
                continue;
            }
            ids.add(itemId);
        }
        Comparator<ResourceLocation> byUsage = Comparator
                .comparingInt((ResourceLocation id) -> usageWeight(usageWeights, id.toString()))
                .reversed()
                .thenComparing(ResourceLocation::toString);
        ids.sort(byUsage);
        return ids;
    }

    private static int usageWeight(Map<String, Integer> usageWeights, String itemId) {
        if (usageWeights == null || usageWeights.isEmpty()) {
            return 0;
        }
        return usageWeights.getOrDefault(itemId, 0);
    }
}
