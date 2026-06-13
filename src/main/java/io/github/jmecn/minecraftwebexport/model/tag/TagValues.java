package io.github.jmecn.minecraftwebexport.model.tag;

import java.util.List;

public record TagValues(List<String> values) {

    public TagValues {
        values = List.copyOf(values == null ? List.of() : values);
    }
}
