package io.github.jmecn.minecraftwebexport.emi.lang;
import io.github.jmecn.minecraftwebexport.pipeline.Hints;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Languages {

    public static final String DEFAULT_LANGUAGE = "en_us";

    private Languages() {
    }

    public static Set<String> resolve(Hints hints) {
        Set<String> fromModules = fromHintLanguages(hints);
        if (!fromModules.isEmpty()) {
            return fromModules;
        }
        return resolveFromSystemProperty();
    }

    public static Set<String> resolve() {
        return resolveFromSystemProperty();
    }

    private static Set<String> fromHintLanguages(Hints hints) {
        if (hints == null || hints.exportLanguages().isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> parsed = new LinkedHashSet<>(hints.exportLanguages());
        parsed.add(DEFAULT_LANGUAGE);
        return Set.copyOf(parsed);
    }

    private static Set<String> resolveFromSystemProperty() {
        String raw = System.getProperty("minecraftWebExport.exportLanguages", "").trim();
        if (raw.isEmpty()) {
            return Set.of(DEFAULT_LANGUAGE);
        }
        Set<String> parsed = java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"*".equals(s))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        parsed.add(DEFAULT_LANGUAGE);
        return Set.copyOf(parsed);
    }
}
