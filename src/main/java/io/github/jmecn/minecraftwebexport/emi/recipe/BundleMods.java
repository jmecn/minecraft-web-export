package io.github.jmecn.minecraftwebexport.emi.recipe;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.ModEntry;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.PackRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BundleMods {



    private final Map<String, ModEntry> mods;

    BundleMods(Map<String, ModEntry> mods) {
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

    public static BundleMods empty() {
        return new BundleMods(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, ModEntry> mods = new TreeMap<>();

        public void put(String namespace, ModEntry entry) {
            mods.put(namespace, entry);
        }

        public BundleMods build() {
            return new BundleMods(mods);
        }
    }
}
