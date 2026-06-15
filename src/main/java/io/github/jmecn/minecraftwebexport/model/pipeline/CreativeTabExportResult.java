package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabEntry;
import java.util.List;
import java.util.Set;

public record CreativeTabExportResult(
        int tabCount, List<CreativeTabEntry> tabs, Set<String> langKeys) {

    public CreativeTabExportResult {
        tabs = List.copyOf(tabs == null ? List.of() : tabs);
        langKeys = Set.copyOf(langKeys == null ? Set.of() : langKeys);
    }

    public static CreativeTabExportResult empty() {
        return new CreativeTabExportResult(0, List.of(), Set.of());
    }
}
