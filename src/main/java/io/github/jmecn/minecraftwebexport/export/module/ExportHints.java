package io.github.jmecn.minecraftwebexport.export.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional export preferences from {@link ExportModule} implementations.
 *
 * <p>{@link #exportLanguages} supplies locale codes for {@code emi/lang/} and {@code items-lang/}.
 * Empty list means defer to {@code -DminecraftWebExport.exportLanguages} (default {@code en_us}).</p>
 */
public record ExportHints(
        Map<String, Integer> itemUsageWeights,
        Map<String, Integer> fluidUsageWeights,
        List<String> namespacePriority,
        boolean exportEntityPreviews,
        List<String> exportLanguages) {

    public ExportHints {
        itemUsageWeights = Map.copyOf(itemUsageWeights == null ? Map.of() : itemUsageWeights);
        fluidUsageWeights = Map.copyOf(fluidUsageWeights == null ? Map.of() : fluidUsageWeights);
        namespacePriority = List.copyOf(namespacePriority == null ? List.of() : namespacePriority);
        exportLanguages = normalizeLanguages(exportLanguages);
    }

    public static ExportHints defaults() {
        return new ExportHints(Map.of(), Map.of(), List.of(), false, List.of());
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
                exportEntityPreviews || other.exportEntityPreviews,
                mergeLanguages(exportLanguages, other.exportLanguages));
    }

    private static List<String> normalizeLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashSet<String> normalized = new java.util.LinkedHashSet<>();
        for (String language : languages) {
            if (language == null) {
                continue;
            }
            String code = language.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
            if (!code.isEmpty() && !"*".equals(code)) {
                normalized.add(code);
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> mergeLanguages(List<String> left, List<String> right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(left);
        merged.addAll(right);
        return List.copyOf(merged);
    }
}
