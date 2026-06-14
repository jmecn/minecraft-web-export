package io.github.jmecn.minecraftwebexport.model.item;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public record ItemIndex(Map<String, List<String>> namespacePaths, List<String> fluidRegistryIds) {

    public ItemIndex {
        namespacePaths = Map.copyOf(namespacePaths == null ? Map.of() : namespacePaths);
        fluidRegistryIds = List.copyOf(fluidRegistryIds == null ? List.of() : fluidRegistryIds);
    }

    /** Registry ids listed in {@link #namespacePaths()} (items only, not fluids). */
    public Set<String> registryItemIds() {
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, List<String>> entry : namespacePaths.entrySet()) {
            String namespace = entry.getKey();
            for (String path : entry.getValue()) {
                if (path == null || path.isEmpty()) {
                    continue;
                }
                ids.add(path.contains(":") ? path : namespace + ":" + path);
            }
        }
        return Set.copyOf(ids);
    }
}
