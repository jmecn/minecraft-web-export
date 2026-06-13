package io.github.jmecn.minecraftwebexport.model.item;

import java.util.List;
import java.util.Map;

public record ItemIndex(Map<String, List<String>> namespacePaths, List<String> fluidRegistryIds) {

    public ItemIndex {
        namespacePaths = Map.copyOf(namespacePaths == null ? Map.of() : namespacePaths);
        fluidRegistryIds = List.copyOf(fluidRegistryIds == null ? List.of() : fluidRegistryIds);
    }
}
