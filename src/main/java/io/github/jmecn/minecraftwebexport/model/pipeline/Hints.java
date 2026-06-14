package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record Hints(List<String> exportLanguages) {

    public Hints {
        exportLanguages = normalizeLanguages(exportLanguages);
    }

    public static Hints defaults() {
        return new Hints(List.of());
    }

    public Hints merge(Hints other) {
        Objects.requireNonNull(other, "other");
        return new Hints(mergeLanguages(exportLanguages, other.exportLanguages));
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
