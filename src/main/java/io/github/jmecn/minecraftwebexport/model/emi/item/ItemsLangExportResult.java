package io.github.jmecn.minecraftwebexport.model.emi.item;

import java.util.List;

public record ItemsLangExportResult(int localeCount, int itemCount, List<String> locales) {

    public static final ItemsLangExportResult EMPTY = new ItemsLangExportResult(0, 0, List.of());
}
