package io.github.jmecn.minecraftwebexport.model.item;

import java.util.Map;

public record NameKeys(int schema, Map<String, String> items) {

    public NameKeys {
        items = Map.copyOf(items == null ? Map.of() : items);
    }

    public static NameKeys of(Map<String, String> items) {
        return new NameKeys(1, items);
    }
}
