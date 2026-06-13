package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ItemIndex(Map<String, List<String>> namespacePaths, List<String> fluidRegistryIds) {

    public ItemIndex {
        namespacePaths = Map.copyOf(namespacePaths == null ? Map.of() : namespacePaths);
        fluidRegistryIds = List.copyOf(fluidRegistryIds == null ? List.of() : fluidRegistryIds);
    }

    public Map<String, Object> toRootMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", Constants.ITEM_INDEX_SCHEMA);
        for (Map.Entry<String, List<String>> entry : namespacePaths.entrySet()) {
            root.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        if (!fluidRegistryIds.isEmpty()) {
            root.put(Constants.FLUID_REGISTRY_IDS_KEY, new ArrayList<>(fluidRegistryIds));
        }
        return root;
    }

    public static ItemIndex fromRootMap(Map<String, Object> root) {
        Objects.requireNonNull(root, "root");
        Map<String, List<String>> namespaces = new LinkedHashMap<>();
        List<String> fluids = List.of();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String key = entry.getKey();
            if ("schema".equals(key)) {
                continue;
            }
            if (Constants.FLUID_REGISTRY_IDS_KEY.equals(key)) {
                fluids = stringList(entry.getValue());
                continue;
            }
            namespaces.put(key, stringList(entry.getValue()));
        }
        return new ItemIndex(namespaces, fluids);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object element : list) {
                if (element != null) {
                    out.add(element.toString());
                }
            }
            return List.copyOf(out);
        }
        return List.of();
    }
}
