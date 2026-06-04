package io.github.jmecn.minecraftwebexport.export.emi;

import io.github.jmecn.minecraftwebexport.export.module.ExportHints;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MinecraftWebExportLanguages {

    public static final String DEFAULT_LANGUAGE = "en_us";

    private MinecraftWebExportLanguages() {
    }

    /**
     * Locale codes for EMI lang merge and {@code items-lang/}.
     * Prefers {@link ExportHints#exportLanguages()} from registered {@link io.github.jmecn.minecraftwebexport.export.module.ExportModule}s,
     * then {@code -DminecraftWebExport.exportLanguages}, then {@code en_us} only.
     */
    public static Set<String> resolve(ExportHints hints) {
        Set<String> fromModules = fromHintLanguages(hints);
        if (!fromModules.isEmpty()) {
            return fromModules;
        }
        return resolveFromSystemProperty();
    }

    /** @see #resolve(ExportHints) */
    public static Set<String> resolve() {
        return resolveFromSystemProperty();
    }

    private static Set<String> fromHintLanguages(ExportHints hints) {
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
