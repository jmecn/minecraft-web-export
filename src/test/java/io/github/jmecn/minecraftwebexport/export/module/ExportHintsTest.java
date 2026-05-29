package io.github.jmecn.minecraftwebexport.export.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportHintsTest {

    @Test
    void mergeCombinesWeightsAndNamespaces() {
        ExportHints left = new ExportHints(
                java.util.Map.of("minecraft:stick", 10),
                java.util.Map.of(),
                java.util.List.of("minecraft"),
                false);
        ExportHints right = new ExportHints(
                java.util.Map.of("minecraft:oak_log", 5),
                java.util.Map.of("minecraft:water", 1),
                java.util.List.of("gtceu"),
                true);

        ExportHints merged = left.merge(right);

        assertEquals(10, merged.itemUsageWeights().get("minecraft:stick"));
        assertEquals(5, merged.itemUsageWeights().get("minecraft:oak_log"));
        assertEquals(1, merged.fluidUsageWeights().get("minecraft:water"));
        assertEquals(java.util.List.of("minecraft", "gtceu"), merged.namespacePriority());
        assertEquals(true, merged.exportEntityPreviews());
    }
}
