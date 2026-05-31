package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * GregTech-style composed registry labels (aligned with {@code emi-recipe-renderer/gtceu-translate.js}).
 */
public final class GtceuRegistryLabels {

    static final String GTCEU = "gtceu";

    private static final Map<String, String> TAG_PREFIX_PATTERN_OVERRIDES = Map.ofEntries(
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
            Map.entry("small_gear", "small_%s_gear"));

    private GtceuRegistryLabels() {
    }

    static boolean isComposedNamespace(String namespace) {
        return GTCEU.equals(namespace);
    }

    static String formatTemplate(String template, String... args) {
        if (template == null) {
            return "";
        }
        int i = 0;
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (true) {
            int next = template.indexOf("%s", idx);
            if (next < 0) {
                out.append(template, idx, template.length());
                break;
            }
            out.append(template, idx, next);
            out.append(i < args.length ? args[i++] : "%s");
            idx = next + 2;
        }
        return out.toString();
    }

    private static String resolveKey(Function<String, String> translateKey, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String value = translateKey.apply(key);
        return value != null && !value.equals(key) ? value : null;
    }

    private static String materialKey(String namespace, String materialPath) {
        return "material." + namespace + "." + materialPath;
    }

    private static boolean langKeyPresent(Map<String, String> langTable, String key) {
        return langTable != null && langTable.containsKey(key);
    }

    private record FluidPath(String storageKey, String materialPath) {
    }

    private static FluidPath parseFluidPath(String path) {
        if (path.startsWith("molten_")) {
            return new FluidPath("molten", path.substring("molten_".length()));
        }
        if (path.endsWith("_plasma")) {
            return new FluidPath("plasma", path.substring(0, path.length() - "_plasma".length()));
        }
        if (path.startsWith("liquid_")) {
            return new FluidPath("liquid", path.substring("liquid_".length()));
        }
        if (path.endsWith("_gas")) {
            return new FluidPath("gas", path.substring(0, path.length() - "_gas".length()));
        }
        return new FluidPath("primary", path);
    }

    private static boolean isLikelyElementMaterial(String materialPath) {
        if (materialPath == null || materialPath.isEmpty() || materialPath.contains("_")) {
            return false;
        }
        return materialPath.matches("^[a-z][a-z0-9]*$");
    }

    private static String pickFluidTemplateKey(String storageKey, String materialPath, Map<String, String> langTable) {
        return switch (storageKey) {
            case "molten" -> "gtceu.fluid.molten";
            case "plasma" -> "gtceu.fluid.plasma";
            case "liquid" -> firstPresent(langTable, "gtceu.fluid.liquid_generic", "gtceu.fluid.generic", "gtceu.fluid.liquid_generic");
            case "gas" -> isLikelyElementMaterial(materialPath)
                    ? firstPresent(langTable, "gtceu.fluid.gas_generic", "gtceu.fluid.generic", "gtceu.fluid.gas_generic")
                    : firstPresent(langTable, "gtceu.fluid.gas_vapor", "gtceu.fluid.gas_generic", "gtceu.fluid.generic", "gtceu.fluid.gas_vapor");
            default -> isLikelyElementMaterial(materialPath)
                    ? firstPresent(langTable, "gtceu.fluid.gas_generic", "gtceu.fluid.generic", "gtceu.fluid.gas_generic")
                    : firstPresent(langTable, "gtceu.fluid.liquid_generic", "gtceu.fluid.gas_vapor", "gtceu.fluid.generic", "gtceu.fluid.liquid_generic");
        };
    }

    private static String firstPresent(Map<String, String> langTable, String... keys) {
        for (String key : keys) {
            if (langKeyPresent(langTable, key)) {
                return key;
            }
        }
        return keys.length > 0 ? keys[keys.length - 1] : "";
    }

    private static String resolveMaterialLabel(
            String namespace,
            String materialPath,
            String fullPath,
            Function<String, String> translateKey) {
        String label = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (label != null) {
            return label;
        }
        return resolveKey(translateKey, materialKey(namespace, fullPath));
    }

    private static String composeFromTemplate(String templateKey, String matLabel, Function<String, String> translateKey) {
        String template = resolveKey(translateKey, templateKey);
        if (template == null || matLabel == null) {
            return null;
        }
        if (!template.contains("%s")) {
            return template;
        }
        return formatTemplate(template, matLabel);
    }

    static String translateComposedFluid(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (!isComposedNamespace(namespace) || path.isEmpty()) {
            return null;
        }
        FluidPath parsed = parseFluidPath(path);
        String matLabel = resolveMaterialLabel(namespace, parsed.materialPath(), path, translateKey);
        if (matLabel == null) {
            return resolveKey(translateKey, materialKey(namespace, path));
        }
        String templateKey = pickFluidTemplateKey(parsed.storageKey(), parsed.materialPath(), langTable);
        String composed = composeFromTemplate(templateKey, matLabel, translateKey);
        if (composed != null) {
            return composed;
        }
        String fallback = resolveKey(translateKey, materialKey(namespace, path));
        return fallback != null ? fallback : resolveKey(translateKey, materialKey(namespace, parsed.materialPath()));
    }

    private record TagPrefixPattern(String langSuffix, String pattern, String langKey) {
    }

    private static List<TagPrefixPattern> buildTagPrefixPatterns(Map<String, String> langTable) {
        Set<String> suffixes = new LinkedHashSet<>(TAG_PREFIX_PATTERN_OVERRIDES.keySet());
        if (langTable != null) {
            for (String key : langTable.keySet()) {
                if (key.startsWith("tagprefix.") && !key.startsWith("tagprefix.polymer.")) {
                    suffixes.add(key.substring("tagprefix.".length()));
                }
            }
        }
        List<TagPrefixPattern> patterns = new ArrayList<>();
        for (String langSuffix : suffixes) {
            String pattern = TAG_PREFIX_PATTERN_OVERRIDES.getOrDefault(langSuffix, "%s_" + langSuffix);
            patterns.add(new TagPrefixPattern(langSuffix, pattern, "tagprefix." + langSuffix));
        }
        patterns.sort(Comparator.comparingInt((TagPrefixPattern p) -> p.pattern().length()).reversed());
        return patterns;
    }

    private static String composeTagPrefixLabel(
            String namespace,
            String materialPath,
            String langSuffix,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (matLabel == null) {
            return null;
        }
        String polymerKey = "tagprefix.polymer." + langSuffix;
        String prefixKey = langKeyPresent(langTable, polymerKey) ? polymerKey : "tagprefix." + langSuffix;
        String prefixTemplate = resolveKey(translateKey, prefixKey);
        if (prefixTemplate == null) {
            return null;
        }
        return formatTemplate(prefixTemplate, matLabel);
    }

    static String tryItemSpecificLang(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (path.isEmpty()) {
            return null;
        }
        String itemKey = "item." + namespace + "." + path;
        String itemTemplate = resolveKey(translateKey, itemKey);
        if (itemTemplate == null) {
            return null;
        }
        if (!itemTemplate.contains("%s")) {
            return itemTemplate;
        }
        for (TagPrefixPattern entry : buildTagPrefixPatterns(langTable)) {
            String materialPath = LangClosureKeys.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
            if (matLabel != null) {
                return formatTemplate(itemTemplate, matLabel);
            }
        }
        String matLabel = resolveMaterialLabelForItemPath(namespace, path, translateKey, langTable);
        return matLabel != null ? formatTemplate(itemTemplate, matLabel) : null;
    }

    private static String resolveMaterialLabelForItemPath(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String direct = resolveKey(translateKey, materialKey(namespace, path));
        if (direct != null) {
            return direct;
        }
        String prefix = "material." + namespace + ".";
        String bestMatPath = null;
        if (langTable == null) {
            return null;
        }
        for (String key : langTable.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String matPath = key.substring(prefix.length());
            if (matPath.isEmpty()) {
                continue;
            }
            if (path.equals(matPath) || path.startsWith(matPath + "_")) {
                if (bestMatPath == null || matPath.length() > bestMatPath.length()) {
                    bestMatPath = matPath;
                }
            }
        }
        if (bestMatPath == null) {
            return null;
        }
        return resolveKey(translateKey, materialKey(namespace, bestMatPath));
    }

    static String translateComposedItem(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (!isComposedNamespace(namespace) || path.isEmpty()) {
            return null;
        }
        String itemOverride = tryItemSpecificLang(namespace, path, translateKey, langTable);
        if (itemOverride != null) {
            return itemOverride;
        }
        String bucketKey = "item." + namespace + ".bucket";
        if (path.endsWith("_bucket") && langKeyPresent(langTable, bucketKey)) {
            String fluidPath = path.substring(0, path.length() - "_bucket".length());
            String bucketTemplate = resolveKey(translateKey, bucketKey);
            String fluidLabel = translateComposedFluid(namespace, fluidPath, translateKey, langTable);
            if (bucketTemplate != null && fluidLabel != null) {
                return formatTemplate(bucketTemplate, fluidLabel);
            }
        }
        for (TagPrefixPattern entry : buildTagPrefixPatterns(langTable)) {
            String materialPath = LangClosureKeys.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String label = composeTagPrefixLabel(namespace, materialPath, entry.langSuffix(), translateKey, langTable);
            if (label != null) {
                return label;
            }
        }
        return null;
    }

    static String translateComposedRegistry(
            String registryId,
            String kind,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        if (colon <= 0 || colon >= bare.length() - 1) {
            return null;
        }
        String namespace = bare.substring(0, colon);
        String path = bare.substring(colon + 1);
        if ("fluid".equals(kind)) {
            return translateComposedFluid(namespace, path, translateKey, langTable);
        }
        if ("item".equals(kind) || "block".equals(kind)) {
            return translateComposedItem(namespace, path, translateKey, langTable);
        }
        return null;
    }
}
