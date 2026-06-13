package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.Constants;

import java.util.List;

public record ItemsLang(int schema, String locale, int itemCount, List<ItemsLangEntry> items) {

    public ItemsLang {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public static ItemsLang of(String locale, List<ItemsLangEntry> items) {
        return new ItemsLang(Constants.ITEMS_LANG_SCHEMA, locale, items.size(), items);
    }
}
