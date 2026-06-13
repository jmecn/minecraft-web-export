package io.github.jmecn.minecraftwebexport.model.emi.item;

import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record ExportedTagCatalog(Set<String> itemTags, Set<String> blockTags, Set<String> fluidTags) {

    public static ExportedTagCatalog empty() {
        return new ExportedTagCatalog(Set.of(), Set.of(), Set.of());
    }

    public RegistryTagSet intersect(RegistryTagSet tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> items = filter(tags.items(), itemTags);
        List<String> blocks = filter(tags.blocks(), blockTags);
        List<String> fluids = filter(tags.fluids(), fluidTags);
        if ((items == null || items.isEmpty())
                && (blocks == null || blocks.isEmpty())
                && (fluids == null || fluids.isEmpty())) {
            return null;
        }
        return new RegistryTagSet(items, blocks, fluids);
    }

    private static List<String> filter(List<String> values, Set<String> allowed) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (allowed.contains(value)) {
                out.add(value);
            }
        }
        return out.isEmpty() ? null : List.copyOf(out);
    }
}
