package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ItemIndexExportResult(int indexedCount, ItemIndex index, Set<String> indexedItemIds) {

    public static ItemIndexExportResult empty() {
        return new ItemIndexExportResult(0, new ItemIndex(Map.of(), List.of()), Set.of());
    }
}
