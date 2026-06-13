package io.github.jmecn.minecraftwebexport.model.tag;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;

public record TagsCatalog(
        int schema,
        List<String> items,
        List<String> blocks,
        List<String> fluids) {

    public TagsCatalog {
        items = items == null ? null : List.copyOf(items);
        blocks = blocks == null ? null : List.copyOf(blocks);
        fluids = fluids == null ? null : List.copyOf(fluids);
    }

    public static TagsCatalog of(List<String> items, List<String> blocks, List<String> fluids) {
        return new TagsCatalog(
                Constants.TAGS_CATALOG_SCHEMA,
                items == null || items.isEmpty() ? null : List.copyOf(items),
                blocks == null || blocks.isEmpty() ? null : List.copyOf(blocks),
                fluids == null || fluids.isEmpty() ? null : List.copyOf(fluids));
    }
}
