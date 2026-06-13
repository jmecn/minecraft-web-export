package io.github.jmecn.minecraftwebexport.model.emi.item;

import java.util.List;

public record SearchIndexResult(int localeCount, int itemCount, List<String> locales) {

    public static final SearchIndexResult EMPTY = new SearchIndexResult(0, 0, List.of());
}
