package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MinecraftWebExportLanguages {

    public static final String DEFAULT_LANGUAGE = "en_us";

    private MinecraftWebExportLanguages() {
    }

    public static Set<String> resolve() {
        String raw = System.getProperty("minecraftWebExport.exportLanguages", "").trim();
        if (raw.isEmpty()) {
            return Set.of(DEFAULT_LANGUAGE);
        }
        Set<String> parsed = java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"*".equals(s))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        parsed.add(DEFAULT_LANGUAGE);
        return Set.copyOf(parsed);
    }
}
