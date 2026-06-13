package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.Constants;

import java.util.Map;

public record ItemDetail(
        int schema,
        Map<String, java.util.List<String>> inputs,
        Map<String, java.util.List<String>> outputs,
        RegistryTagSet tags,
        RegistryTagSet tagsInBundle) {

    public static ItemDetail of(
            Map<String, java.util.List<String>> inputs,
            Map<String, java.util.List<String>> outputs,
            RegistryTagSet tags,
            RegistryTagSet tagsInBundle) {
        return new ItemDetail(Constants.ITEM_DETAIL_SCHEMA, inputs, outputs, tags, tagsInBundle);
    }
}
