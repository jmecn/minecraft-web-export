package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;
import java.util.Map;

public record ItemDetail(
        int schema,
        Map<String, List<String>> inputs,
        Map<String, List<String>> outputs,
        RegistryTagSet tags,
        RegistryTagSet tagsInBundle) {

    public static ItemDetail of(
            Map<String, List<String>> inputs,
            Map<String, List<String>> outputs,
            RegistryTagSet tags,
            RegistryTagSet tagsInBundle) {
        return new ItemDetail(
                Constants.ITEM_DETAIL_SCHEMA,
                emptyToNull(inputs),
                emptyToNull(outputs),
                emptyToNull(tags),
                emptyToNull(tagsInBundle));
    }

    private static Map<String, List<String>> emptyToNull(Map<String, List<String>> map) {
        return map == null || map.isEmpty() ? null : map;
    }

    private static RegistryTagSet emptyToNull(RegistryTagSet tags) {
        return tags == null || tags.isEmpty() ? null : tags;
    }
}
