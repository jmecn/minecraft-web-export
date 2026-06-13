package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record Hints(
        Map<String, Integer> itemUsageWeights,
        Map<String, Integer> fluidUsageWeights,
        List<String> namespacePriority,
        boolean exportEntityPreviews,
        List<String> exportLanguages) {

    public Hints {
        itemUsageWeights = Map.copyOf(itemUsageWeights == null ? Map.of() : itemUsageWeights);
        fluidUsageWeights = Map.copyOf(fluidUsageWeights == null ? Map.of() : fluidUsageWeights);
        namespacePriority = List.copyOf(namespacePriority == null ? List.of() : namespacePriority);
        exportLanguages = normalizeLanguages(exportLanguages);
    }

    public static Hints defaults() {
        return new Hints(Map.of(), Map.of(), List.of(), false, List.of());
    }

    public Hints merge(Hints other) {
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
        return new Hints(
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
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String language : languages) {
            if (language == null) {
                continue;
            }
            String code = language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
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
        LinkedHashSet<String> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return List.copyOf(merged);
    }
}
