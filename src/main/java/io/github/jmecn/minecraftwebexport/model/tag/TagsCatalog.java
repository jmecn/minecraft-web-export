package io.github.jmecn.minecraftwebexport.model.tag;

import io.github.jmecn.minecraftwebexport.Constants;

import java.util.List;

public record TagsCatalog(
        int schema,
        List<String> items,
        List<String> blocks,
        List<String> fluids) {

    public TagsCatalog {
        items = List.copyOf(items == null ? List.of() : items);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
        fluids = List.copyOf(fluids == null ? List.of() : fluids);
    }

    public static TagsCatalog of(List<String> items, List<String> blocks, List<String> fluids) {
        return new TagsCatalog(Constants.TAGS_CATALOG_SCHEMA, items, blocks, fluids);
    }
}
