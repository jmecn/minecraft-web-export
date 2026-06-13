package io.github.jmecn.minecraftwebexport.emi.lang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class GtceuLabels {

    private record GtToolPattern(String toolName, String pattern, String templateKey) {
    }

    private GtceuLabels() {
    }

    static boolean isComposedNamespace(String namespace) {
        return GtMaterialPatterns.COMPOSED_NAMESPACES.contains(namespace);
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

    static String resolveBucketTemplateKey(String namespace, Map<String, String> langTable) {
        String own = "item." + namespace + ".bucket";
        if (langKeyPresent(langTable, own)) {
            return own;
        }
        if ("tfg".equals(namespace) && langKeyPresent(langTable, "item.gtceu.bucket")) {
            return "item.gtceu.bucket";
        }
        return null;
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

    private static String pickFluidTemplateKey(
            String storageKey, String materialPath, String namespace, Map<String, String> langTable) {
        String normalizedStorage = GtMaterialFacts.normalizeStorageKey(storageKey);
        Optional<String> gtKey =
                GtMaterialFacts.fluidTranslationKey(namespace, materialPath, normalizedStorage);
        if (gtKey.isPresent()) {
            return gtKey.get();
        }

        boolean modpackFluid = !GtMaterialPatterns.GTCEU.equals(namespace);
        return switch (normalizedStorage) {
            case "molten" -> "gtceu.fluid.molten";
            case "plasma" -> "gtceu.fluid.plasma";
            case "liquid" -> firstPresent(langTable, "gtceu.fluid.liquid_generic", "gtceu.fluid.generic", "gtceu.fluid.liquid_generic");
            case "gas", "primary" -> pickGenericFluidTemplate(langTable);
            default -> pickGenericFluidTemplate(langTable);
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
        String flatFluid = resolveKey(translateKey, "fluid." + namespace + "." + path);
        if (flatFluid != null) {
            return flatFluid;
        }
        FluidPath parsed = parseFluidPath(path);
        String matLabel = resolveMaterialLabel(namespace, parsed.materialPath(), path, translateKey);
        if (matLabel == null) {
            return resolveKey(translateKey, materialKey(namespace, path));
        }
        String templateKey = pickFluidTemplateKey(parsed.storageKey(), parsed.materialPath(), namespace, langTable);
        String composed = composeFromTemplate(templateKey, matLabel, translateKey);
        if (composed != null) {
            return composed;
        }
        String fallback = resolveKey(translateKey, materialKey(namespace, path));
        return fallback != null ? fallback : resolveKey(translateKey, materialKey(namespace, parsed.materialPath()));
    }

    private record TagPrefixPattern(String langSuffix, String pattern, String langKey) {
    }

    private static List<GtToolPattern> buildGtToolPatterns(Map<String, String> langTable) {
        List<GtToolPattern> patterns = new ArrayList<>();
        if (langTable == null) {
            return patterns;
        }
        String prefix = "item.gtceu.tool.";
        Set<String> seen = new LinkedHashSet<>();
        for (String key : langTable.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String toolName = key.substring(prefix.length());
            if (toolName.isEmpty() || toolName.contains(".") || !seen.add(toolName)) {
                continue;
            }
            String template = langTable.get(key);
            if (template == null || !template.contains("%s")) {
                seen.remove(toolName);
                continue;
            }
            patterns.add(new GtToolPattern(toolName, GtMaterialPatterns.defaultGtToolIdPattern(toolName), key));
        }
        for (String toolName : GtMaterialPatterns.GTMUTILS_ELECTRIC_TOOL_NAMES) {
            if (!seen.add(toolName)) {
                continue;
            }
            String key = prefix + toolName;
            String template = langTable.get(key);
            if (template == null || !template.contains("%s")) {
                seen.remove(toolName);
                continue;
            }
            patterns.add(new GtToolPattern(toolName, GtMaterialPatterns.defaultGtToolIdPattern(toolName), key));
        }
        patterns.sort(Comparator.comparingInt((GtToolPattern p) -> p.pattern().length()).reversed());
        return patterns;
    }

    private static String resolveGtToolTemplateKey(
            String namespace, String toolName, Map<String, String> langTable) {
        if (langTable == null) {
            return null;
        }
        String own = "item." + namespace + ".tool." + toolName;
        if (langTable.containsKey(own)) {
            return own;
        }
        String gtceu = "item.gtceu.tool." + toolName;
        if (!GtMaterialPatterns.GTCEU.equals(namespace) && langTable.containsKey(gtceu)) {
            return gtceu;
        }
        return null;
    }

    private static String translateGtToolItem(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        for (GtToolPattern entry : buildGtToolPatterns(langTable)) {
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String templateKey = resolveGtToolTemplateKey(namespace, entry.toolName(), langTable);
            if (templateKey == null) {
                templateKey = entry.templateKey();
            }
            String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
            if (matLabel == null) {
                continue;
            }
            String composed = composeFromTemplate(templateKey, matLabel, translateKey);
            if (composed != null) {
                return composed;
            }
        }
        return null;
    }

    private static String translateBudIndicator(
            String namespace, String path, Function<String, String> translateKey) {
        if (!path.endsWith("_bud_indicator")) {
            return null;
        }
        String materialPath = path.substring(0, path.length() - "_bud_indicator".length());
        if (materialPath.isEmpty()) {
            return null;
        }
        String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (matLabel == null) {
            return null;
        }
        return composeFromTemplate("block.bud_indicator", matLabel, translateKey);
    }

    private static List<TagPrefixPattern> buildTagPrefixPatterns(Map<String, String> langTable) {
        Set<String> suffixes = new LinkedHashSet<>(GtMaterialPatterns.TAG_PREFIX_PATTERNS.keySet());
        if (langTable != null) {
            for (String key : langTable.keySet()) {
                if (key.startsWith("tagprefix.") && !key.startsWith("tagprefix.polymer.")) {
                    suffixes.add(key.substring("tagprefix.".length()));
                }
            }
        }
        List<TagPrefixPattern> patterns = new ArrayList<>();
        for (String langSuffix : suffixes) {
            String pattern = GtMaterialPatterns.TAG_PREFIX_PATTERNS.getOrDefault(langSuffix, "%s_" + langSuffix);
            patterns.add(new TagPrefixPattern(langSuffix, pattern, "tagprefix." + langSuffix));
        }
        patterns.sort(Comparator.comparingInt((TagPrefixPattern p) -> p.pattern().length()).reversed());
        return patterns;
    }

    private static String pickGenericFluidTemplate(Map<String, String> langTable) {
        return firstPresent(langTable, "gtceu.fluid.generic", "gtceu.fluid.liquid_generic", "gtceu.fluid.generic");
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
        String prefixKey = GtMaterialFacts.tagPrefixLangKey(langSuffix, namespace, materialPath, langTable);
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
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
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
        String budLabel = translateBudIndicator(namespace, path, translateKey);
        if (budLabel != null) {
            return budLabel;
        }
        String toolLabel = translateGtToolItem(namespace, path, translateKey, langTable);
        if (toolLabel != null) {
            return toolLabel;
        }
        String bucketTemplateKey = resolveBucketTemplateKey(namespace, langTable);
        if (path.endsWith("_bucket") && bucketTemplateKey != null) {
            String fluidPath = path.substring(0, path.length() - "_bucket".length());
            String bucketTemplate = resolveKey(translateKey, bucketTemplateKey);
            String fluidLabel = translateComposedFluid(namespace, fluidPath, translateKey, langTable);
            if (bucketTemplate != null && fluidLabel != null) {
                return formatTemplate(bucketTemplate, fluidLabel);
            }
        }
        for (TagPrefixPattern entry : buildTagPrefixPatterns(langTable)) {
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String label = composeTagPrefixLabel(namespace, materialPath, entry.langSuffix(), translateKey, langTable);
            if (label != null) {
                return label;
            }
        }
        String itemOverride = tryItemSpecificLang(namespace, path, translateKey, langTable);
        return itemOverride;
    }

    static String translateComposedRegistry(
            String registryId,
            String kind,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String bare = RegistryKeys.normalizeRegistryId(registryId);
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
