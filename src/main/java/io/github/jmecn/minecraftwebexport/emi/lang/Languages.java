package io.github.jmecn.minecraftwebexport.emi.lang;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Languages {

    public static final String DEFAULT_LANGUAGE = Constants.DEFAULT_LANGUAGE;

    private Languages() {}

    public static Set<String> resolve(Hints hints) {
        Set<String> fromModules = fromHintLanguages(hints);
        if (!fromModules.isEmpty()) {
            return fromModules;
        }
        return resolveFromConfig();
    }

    public static Set<String> resolve() {
        return resolveFromConfig();
    }

    private static Set<String> fromHintLanguages(Hints hints) {
        if (hints == null || hints.exportLanguages().isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> parsed = new LinkedHashSet<>(hints.exportLanguages());
        parsed.add(DEFAULT_LANGUAGE);
        return Set.copyOf(parsed);
    }

    private static Set<String> resolveFromConfig() {
        return parseLanguageList(MweConfig.exportLanguages());
    }

    static Set<String> parseLanguageList(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return Set.of(DEFAULT_LANGUAGE);
        }
        Set<String> parsed = Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"*".equals(s))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        parsed.add(DEFAULT_LANGUAGE);
        return Set.copyOf(parsed);
    }
}
