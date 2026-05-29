package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Per-namespace route file list + layout pack catalog for {@code bundle.json#mods}. */
public final class RecipeBundleMods {

    public record PackRef(String file, long bytes) {
    }

    public record ModEntry(List<String> routes, List<PackRef> packs) {
    }

    private final Map<String, ModEntry> mods;

    RecipeBundleMods(Map<String, ModEntry> mods) {
        this.mods = Map.copyOf(mods);
    }

    public Map<String, ModEntry> mods() {
        return mods;
    }

    public boolean isEmpty() {
        return mods.isEmpty();
    }

    Map<String, Object> toBundleJsonMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, ModEntry> entry : mods.entrySet()) {
            ModEntry mod = entry.getValue();
            Map<String, Object> modObject = new LinkedHashMap<>();
            modObject.put("routes", List.copyOf(mod.routes()));
            List<Map<String, Object>> packs = new ArrayList<>();
            for (PackRef pack : mod.packs()) {
                Map<String, Object> packObject = new LinkedHashMap<>();
                packObject.put("file", pack.file());
                packObject.put("bytes", pack.bytes());
                packs.add(packObject);
            }
            modObject.put("packs", packs);
            out.put(entry.getKey(), modObject);
        }
        return out;
    }

    static RecipeBundleMods empty() {
        return new RecipeBundleMods(Map.of());
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private final Map<String, ModEntry> mods = new TreeMap<>();

        void put(String namespace, ModEntry entry) {
            mods.put(namespace, entry);
        }

        RecipeBundleMods build() {
            return new RecipeBundleMods(mods);
        }
    }
}
