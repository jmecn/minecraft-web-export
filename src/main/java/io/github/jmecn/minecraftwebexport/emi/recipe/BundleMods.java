package io.github.jmecn.minecraftwebexport.emi.recipe;

import io.github.jmecn.minecraftwebexport.model.emi.recipe.ModEntry;
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
