package io.github.jmecn.minecraftwebexport.emi.lang;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ClosureKeys {

    private ClosureKeys() {}

    public static void addForItem(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryKeys.itemLookupKeys(registryId));
        addComposedMaterialItemKeys(into, registryId);
    }

    public static void addForFluid(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryKeys.fluidLookupKeys(registryId));
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
        String namespace = RegistryKeys.namespace(registryId);
        if (!GtMaterialPatterns.COMPOSED_NAMESPACES.contains(namespace)) {
            return;
        }
        String path = path(registryId);
        if (path.isEmpty()) {
            return;
        }
        if (path.endsWith("_bucket")) {
            into.add("item." + namespace + ".bucket");
            if ("tfg".equals(namespace)) {
                into.add("item.gtceu.bucket");
            }
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_bucket".length()));
            return;
        }
        if (path.endsWith("_bud_indicator")) {
            into.add("block.bud_indicator");
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_bud_indicator".length()));
            return;
        }
        if (addGtToolClosureKeys(into, namespace, path)) {
            return;
        }
        for (var entry : GtMaterialPatterns.TAG_PREFIX_PATTERNS.entrySet()) {
            String material = GtMaterialPatterns.extractMaterial(path, entry.getValue());
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
            String material = GtMaterialPatterns.extractMaterial(path, "%s_" + suffix);
            if (material != null && !material.isEmpty()) {
                into.add("tagprefix." + suffix);
                into.add("tagprefix.polymer." + suffix);
                into.add("material." + namespace + "." + material);
            }
        }
    }

    private static void addComposedMaterialFluidKeys(Set<String> into, String registryId) {
        String namespace = RegistryKeys.namespace(registryId);
        if (!GtMaterialPatterns.COMPOSED_NAMESPACES.contains(namespace)) {
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
        String bare = RegistryKeys.normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        return colon >= 0 ? bare.substring(colon + 1) : bare;
    }

    private static boolean addGtToolClosureKeys(Set<String> into, String namespace, String path) {
        for (var entry : GtMaterialPatterns.orderedToolPatterns()) {
            String material = GtMaterialPatterns.extractMaterial(path, entry.getValue());
            if (material == null || material.isEmpty()) {
                continue;
            }
            into.add("item.gtceu.tool." + entry.getKey());
            into.add("material." + namespace + "." + material);
            return true;
        }
        return false;
    }

    public static String extractMaterial(String path, String pattern) {
        return GtMaterialPatterns.extractMaterial(path, pattern);
    }

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

    public static void addForTag(Set<String> into, String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return;
        }
        String dotted = RegistryKeys.dottedRegistryId(tagId);
        if (dotted.isEmpty()) {
            return;
        }
        into.add("tag.item." + dotted);
        into.add("tag.block." + dotted);
        into.add("tag.fluid." + dotted);
    }

    public static Set<String> mergeTagLangKeys(Set<String> seedLangKeys, Set<String> tagIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String tagId : tagIds == null ? Set.<String>of() : tagIds) {
            addForTag(merged, tagId);
        }
        return Set.copyOf(merged);
    }
}
