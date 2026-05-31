package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves registry ids to display labels (aligned with emi-recipe-renderer/registry-label.mjs). */
public final class RegistryLabelResolver {

    private final Map<String, String> current;
    private final Map<String, String> fallback;
    private final Map<String, String> merged;

    public RegistryLabelResolver(Map<String, String> current, Map<String, String> fallback) {
        this.current = current == null ? Map.of() : current;
        this.fallback = fallback == null ? Map.of() : fallback;
        this.merged = new HashMap<>(this.fallback);
        this.merged.putAll(this.current);
    }

    public String translate(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (current.containsKey(key)) {
            return current.get(key);
        }
        if (fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return key;
    }

    public String translateRegistry(String registryId) {
        return translateRegistry(registryId, "item");
    }

    public String translateRegistry(String registryId, String kind) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        String namespace = RegistryLangKeys.namespace(bare);

        if (GtceuRegistryLabels.isComposedNamespace(namespace)
                && ("item".equals(kind) || "block".equals(kind) || "fluid".equals(kind))) {
            String composedFirst = GtceuRegistryLabels.translateComposedRegistry(bare, kind, this::translate, merged);
            if (composedFirst != null) {
                return composedFirst;
            }
        }

        List<String> candidates = switch (kind) {
            case "fluid" -> RegistryLangKeys.fluidLookupKeys(registryId);
            case "block" -> List.of(
                    RegistryLangKeys.blockKey(registryId),
                    RegistryLangKeys.itemKey(registryId),
                    RegistryLangKeys.fluidKey(registryId));
            default -> RegistryLangKeys.itemLookupKeys(registryId);
        };
        for (String candidate : candidates) {
            String label = translate(candidate);
            if (!candidate.equals(label)) {
                return label;
            }
        }

        String composed = GtceuRegistryLabels.translateComposedRegistry(bare, kind, this::translate, merged);
        if (composed != null) {
            return composed;
        }

        return bare.isEmpty() ? String.valueOf(registryId) : bare;
    }
}
