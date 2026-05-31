package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lang keys to request from mod lang files ({@code assets/<mod>/lang/<locale>.json}) for scoped export closure.
 * Aligned with {@code emi-bundle-optimize/gtceu-composed-keys.mjs} and {@code emi-recipe-renderer}.
 */
public final class LangClosureKeys {

    private static final String GTCEU = "gtceu";
    private static final java.util.Set<String> COMPOSED_MATERIAL_NAMESPACES = java.util.Set.of(GTCEU, "tfg");

    private static final Map<String, String> GTCEU_TAG_PREFIX_PATTERNS = Map.ofEntries(
            Map.entry("raw", "raw_%s"),
            Map.entry("raw_ore_block", "raw_%s_block"),
            Map.entry("refined_ore", "refined_%s_ore"),
            Map.entry("purified_ore", "purified_%s_ore"),
            Map.entry("crushed_ore", "crushed_%s_ore"),
            Map.entry("hot_ingot", "hot_%s_ingot"),
            Map.entry("chipped_gem", "chipped_%s_gem"),
            Map.entry("flawed_gem", "flawed_%s_gem"),
            Map.entry("flawless_gem", "flawless_%s_gem"),
            Map.entry("exquisite_gem", "exquisite_%s_gem"),
            Map.entry("small_dust", "small_%s_dust"),
            Map.entry("tiny_dust", "tiny_%s_dust"),
            Map.entry("impure_dust", "impure_%s_dust"),
            Map.entry("pure_dust", "pure_%s_dust"),
            Map.entry("dense_plate", "dense_%s_plate"),
            Map.entry("double_plate", "double_%s_plate"),
            Map.entry("long_rod", "long_%s_rod"),
            Map.entry("small_spring", "small_%s_spring"),
            Map.entry("fine_wire", "fine_%s_wire"),
            Map.entry("wire_gt_single", "%s_single_wire"),
            Map.entry("wire_gt_double", "%s_double_wire"),
            Map.entry("wire_gt_quadruple", "%s_quadruple_wire"),
            Map.entry("wire_gt_octal", "%s_octal_wire"),
            Map.entry("wire_gt_hex", "%s_hex_wire"),
            Map.entry("cable_gt_single", "%s_single_cable"),
            Map.entry("cable_gt_double", "%s_double_cable"),
            Map.entry("cable_gt_quadruple", "%s_quadruple_cable"),
            Map.entry("cable_gt_octal", "%s_octal_cable"),
            Map.entry("cable_gt_hex", "%s_hex_cable"),
            Map.entry("small_gear", "small_%s_gear"));

    private LangClosureKeys() {
    }

    public static void addForItem(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryLangKeys.itemLookupKeys(registryId));
        addComposedMaterialItemKeys(into, registryId);
    }

    public static void addForFluid(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryLangKeys.fluidLookupKeys(registryId));
        addComposedMaterialFluidKeys(into, registryId);
    }

    private static void addLookupKeys(Set<String> into, java.util.List<String> keys) {
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                into.add(key);
            }
        }
    }

    private static void addComposedMaterialItemKeys(Set<String> into, String registryId) {
        String namespace = RegistryLangKeys.namespace(registryId);
        if (!COMPOSED_MATERIAL_NAMESPACES.contains(namespace)) {
            return;
        }
        String path = path(registryId);
        if (path.isEmpty()) {
            return;
        }
        if (path.endsWith("_bucket")) {
            into.add("item." + namespace + ".bucket");
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_bucket".length()));
            return;
        }
        for (var entry : GTCEU_TAG_PREFIX_PATTERNS.entrySet()) {
            String material = extractMaterial(path, entry.getValue());
            if (material == null) {
                continue;
            }
            into.add("tagprefix." + entry.getKey());
            into.add("tagprefix.polymer." + entry.getKey());
            into.add("material." + namespace + "." + material);
            return;
        }
        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String suffix = path.substring(lastUnderscore + 1);
            String material = extractMaterial(path, "%s_" + suffix);
            if (material != null && !material.isEmpty()) {
                into.add("tagprefix." + suffix);
                into.add("tagprefix.polymer." + suffix);
                into.add("material." + namespace + "." + material);
            }
        }
    }

    private static void addComposedMaterialFluidKeys(Set<String> into, String registryId) {
        String namespace = RegistryLangKeys.namespace(registryId);
        if (!COMPOSED_MATERIAL_NAMESPACES.contains(namespace)) {
            return;
        }
        String path = path(registryId);
        if (path.isEmpty()) {
            return;
        }
        into.add("gtceu.fluid.liquid_generic");
        into.add("gtceu.fluid.generic");
        into.add("gtceu.fluid.molten");
        into.add("gtceu.fluid.plasma");
        into.add("gtceu.fluid.gas_vapor");
        into.add("gtceu.fluid.gas_generic");
        if (path.startsWith("molten_")) {
            into.add("material." + namespace + "." + path.substring("molten_".length()));
        } else if (path.endsWith("_plasma")) {
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_plasma".length()));
        } else if (path.startsWith("liquid_")) {
            into.add("material." + namespace + "." + path.substring("liquid_".length()));
        } else if (path.endsWith("_gas")) {
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_gas".length()));
        } else {
            into.add("material." + namespace + "." + path);
        }
    }

    private static String path(String registryId) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        return colon >= 0 ? bare.substring(colon + 1) : bare;
    }

    static String extractMaterial(String path, String pattern) {
        if (path == null || pattern == null || !pattern.contains("%s")) {
            return null;
        }
        if (pattern.startsWith("%s_")) {
            String suffix = pattern.substring(2);
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
            return null;
        }
        if (pattern.endsWith("_%s")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
            return null;
        }
        int idx = pattern.indexOf("%s");
        String before = pattern.substring(0, idx);
        String after = pattern.substring(idx + 2);
        if (path.startsWith(before) && path.endsWith(after)) {
            return path.substring(before.length(), path.length() - after.length());
        }
        return null;
    }

    /** Merge seed lang keys with closure item/fluid registry lookups. */
    public static Set<String> mergeClosureLangKeys(
            Set<String> seedLangKeys, Set<String> itemIds, Set<String> fluidIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String itemId : itemIds == null ? Set.<String>of() : itemIds) {
            addForItem(merged, itemId);
        }
        for (String fluidId : fluidIds == null ? Set.<String>of() : fluidIds) {
            addForFluid(merged, fluidId);
        }
        return Set.copyOf(merged);
    }
}
