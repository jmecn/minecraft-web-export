package io.github.jmecn.minecraftwebexport.model.item;

import java.util.Collection;
import java.util.List;

public record RegistryTagSet(List<String> items, List<String> blocks, List<String> fluids) {

    public RegistryTagSet {
        items = items == null ? null : List.copyOf(items);
        blocks = blocks == null ? null : List.copyOf(blocks);
        fluids = fluids == null ? null : List.copyOf(fluids);
    }

    public boolean isEmpty() {
        return (items == null || items.isEmpty())
                && (blocks == null || blocks.isEmpty())
                && (fluids == null || fluids.isEmpty());
    }

    public static RegistryTagSet empty() {
        return new RegistryTagSet(null, null, null);
    }

    public static RegistryTagSet of(Collection<String> items, Collection<String> blocks, Collection<String> fluids) {
        return new RegistryTagSet(
                copyOrNull(items),
                copyOrNull(blocks),
                copyOrNull(fluids));
    }

    private static List<String> copyOrNull(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return List.copyOf(values);
    }
}
