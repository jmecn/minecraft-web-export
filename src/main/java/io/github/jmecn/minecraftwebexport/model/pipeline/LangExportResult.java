package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.List;
import java.util.Map;

public record LangExportResult(
        int languagesWritten,
        List<String> locales,
        Map<String, Map<String, String>> tablesByLocale) {

    public static LangExportResult empty() {
        return new LangExportResult(0, List.of(), Map.of());
    }
}
