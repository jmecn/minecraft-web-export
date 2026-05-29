package io.github.jmecn.minecraftwebexport.export.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional export preferences from {@link ExportModule} implementations.
 */
public record ExportHints(
        Map<String, Integer> itemUsageWeights,
        Map<String, Integer> fluidUsageWeights,
        List<String> namespacePriority,
        boolean exportEntityPreviews) {

    public ExportHints {
        itemUsageWeights = Map.copyOf(itemUsageWeights == null ? Map.of() : itemUsageWeights);
        fluidUsageWeights = Map.copyOf(fluidUsageWeights == null ? Map.of() : fluidUsageWeights);
        namespacePriority = List.copyOf(namespacePriority == null ? List.of() : namespacePriority);
    }

    public static ExportHints defaults() {
        return new ExportHints(Map.of(), Map.of(), List.of(), false);
    }

    public ExportHints merge(ExportHints other) {
        Objects.requireNonNull(other, "other");
        Map<String, Integer> items = new LinkedHashMap<>(itemUsageWeights);
        other.itemUsageWeights.forEach(items::putIfAbsent);
        Map<String, Integer> fluids = new LinkedHashMap<>(fluidUsageWeights);
        other.fluidUsageWeights.forEach(fluids::putIfAbsent);
        List<String> namespaces = new ArrayList<>(namespacePriority);
        for (String ns : other.namespacePriority) {
            if (!namespaces.contains(ns)) {
                namespaces.add(ns);
            }
        }
        return new ExportHints(
                items,
                fluids,
                namespaces,
                exportEntityPreviews || other.exportEntityPreviews);
    }
}
