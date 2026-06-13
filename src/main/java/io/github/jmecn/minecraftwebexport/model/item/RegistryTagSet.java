package io.github.jmecn.minecraftwebexport.model.item;

import java.util.ArrayList;
import java.util.List;

public record RegistryTagSet(List<String> items, List<String> blocks, List<String> fluids) {

    public RegistryTagSet {
        items = List.copyOf(items == null ? List.of() : items);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
        fluids = List.copyOf(fluids == null ? List.of() : fluids);
    }

    public boolean isEmpty() {
        return items.isEmpty() && blocks.isEmpty() && fluids.isEmpty();
    }

    public static RegistryTagSet empty() {
        return new RegistryTagSet(List.of(), List.of(), List.of());
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (!items.isEmpty()) {
            map.put("items", new ArrayList<>(items));
        }
        if (!blocks.isEmpty()) {
            map.put("blocks", new ArrayList<>(blocks));
        }
        if (!fluids.isEmpty()) {
            map.put("fluids", new ArrayList<>(fluids));
        }
        return map;
    }
}
