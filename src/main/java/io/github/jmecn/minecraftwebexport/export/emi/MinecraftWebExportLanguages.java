package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MinecraftWebExportLanguages {

    public static final List<String> SUPPORTED = List.of(
            "en_us",
            "de_de",
            "es_es",
            "fr_fr",
            "hu_hu",
            "ja_jp",
            "ko_kr",
            "pl_pl",
            "pt_br",
            "ru_ru",
            "sv_se",
            "tr_tr",
            "uk_ua",
            "zh_cn",
            "zh_hk",
            "zh_tw");

    private MinecraftWebExportLanguages() {
    }

    public static Set<String> resolve() {
        String raw = System.getProperty("minecraftWebExport.exportLanguages", "").trim();
        if (raw.isEmpty()) {
            return Set.copyOf(SUPPORTED);
        }
        if ("*".equals(raw)) {
            return null;
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
