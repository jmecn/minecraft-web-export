package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import io.github.jmecn.minecraftwebexport.export.module.ExportHints;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Merges mod language files the same way {@link net.minecraft.client.resources.language.ClientLanguage}
 * does at runtime: {@link ResourceManager#listResourceStacks} per {@code assets/<ns>/lang/<locale>.json},
 * lower-priority packs first, later packs override individual keys (KubeJS partial overrides included).
 */
public final class LangMergerExporter {

    private static final Logger LOGGER = LogManager.getLogger(LangMergerExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private LangMergerExporter() {
    }

    public record Result(
            int languagesWritten,
            long totalBytes,
            int duplicateKeyWarnings,
            int closureKeysRequested,
            int keysSkipped,
            int keysPerLanguage) {
    }

    static final class MergeStats {
        int keysSkipped;
        int duplicateKeyWarnings;
        int resourceLayersRead;
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipLangExport");
    }

    /** FULL export: merge only closure keys (+ GTCEu / emi.category templates). */
    public static boolean isLangPruneEnabled() {
        return isEnabled() && !Boolean.getBoolean("minecraftWebExport.skipLangPruneExport");
    }

    static boolean shouldMergeLangKey(String key, Set<String> onlyKeys) {
        if (onlyKeys == null) {
            return true;
        }
        if (onlyKeys.contains(key)) {
            return true;
        }
        return isEmiCategoryLangKey(key);
    }

    /**
     * Keys kept in CDN {@code lang/} after FULL prune: EMI categories, tags — not registry compose tables
     * ({@code material.*}, {@code tagprefix.*}, …) which live in {@code items-lang}.
     */
    public static Set<String> filterWebDeployKeys(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return keys;
        }
        java.util.TreeSet<String> out = new java.util.TreeSet<>();
        for (String key : keys) {
            if (isWebDeployLangKey(key)) {
                out.add(key);
            }
        }
        return out;
    }

    static boolean isWebDeployLangKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        if (key.startsWith("tag.item.")
                || key.startsWith("tag.block.")
                || key.startsWith("tag.fluid.")) {
            return true;
        }
        if (key.startsWith("emi.category.")) {
            return true;
        }
        if (key.startsWith("material.")
                || key.startsWith("tagprefix.")
                || key.startsWith("gtceu.fluid.")) {
            return false;
        }
        return !key.startsWith("item.") && !key.startsWith("block.") && !key.startsWith("fluid.");
    }

    private static boolean isGtceuTranslationKey(String key) {
        return key.startsWith("material.gtceu.")
                || key.startsWith("material.tfg.")
                || key.startsWith("tagprefix.")
                || key.startsWith("gtceu.fluid.")
                || key.equals("item.gtceu.bucket")
                || key.equals("item.tfg.bucket");
    }

    private static boolean isEmiCategoryLangKey(String key) {
        return key.startsWith("emi.category.");
    }

    public static Result exportEmiLang(Path outputDir, Minecraft client, Set<String> onlyKeys, ExportHints hints)
            throws IOException {
        return exportTo(EmiBundlePaths.resolve(outputDir, EmiBundlePaths.LANG_DIR), client, null, onlyKeys, hints);
    }

    /** @deprecated use {@link #exportEmiLang(Path, Minecraft, Set, ExportHints)} */
    @Deprecated
    public static Result exportEmiLang(Path outputDir, Minecraft client, Set<String> onlyKeys) throws IOException {
        return exportEmiLang(outputDir, client, onlyKeys, ExportHints.defaults());
    }

    public static Result exportTo(Path langRoot, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
            throws IOException {
        return exportTo(langRoot, client, onlyNamespaces, onlyKeys, ExportHints.defaults());
    }

    public static Result exportTo(
            Path langRoot,
            Minecraft client,
            Set<String> onlyNamespaces,
            Set<String> onlyKeys,
            ExportHints hints) throws IOException {
        Files.createDirectories(langRoot);

        Set<String> languages = MinecraftWebExportLanguages.resolve(hints);
        if (languages.isEmpty()) {
            languages = client.getLanguageManager().getLanguages().keySet();
        }

        int languagesWritten = 0;
        long totalBytes = 0;
        int duplicateWarnings = 0;
        int keysSkipped = 0;
        int keysPerLanguage = 0;
        String mode = onlyKeys == null ? "full" : "closure";

        for (String langCode : languages) {
            String langFile = langCode + ".json";
            Map<String, String> merged = new TreeMap<>();
            Map<ResourceLocation, List<Resource>> stacks = collectLangStacks(client, langFile, onlyNamespaces);
            MergeStats stats = new MergeStats();

            if (stacks.isEmpty()) {
                LOGGER.warn(
                        "{} {} - no mod lang files matched (namespaces={})",
                        ExportLog.LANG,
                        langCode,
                        onlyNamespaces == null ? "all" : onlyNamespaces);
                logLangPathProbe(client, langFile);
            } else {
                mergeLangStacksInto(merged, stacks, onlyKeys, stats);
            }

            if (onlyKeys != null) {
                VanillaMinecraftLangSupplement.supplement(merged, client, langCode, onlyKeys);
            }

            if (merged.isEmpty()) {
                LOGGER.warn(
                        "{} {} - 0 keys after merge ({}, {} lang file stacks, {} pack layers)",
                        ExportLog.LANG,
                        langCode,
                        mode,
                        stacks.size(),
                        stats.resourceLayersRead);
                continue;
            }

            Path out = langRoot.resolve(langFile);
            String json = GSON.toJson(merged);
            Files.writeString(out, json);
            languagesWritten++;
            totalBytes += json.length();
            keysPerLanguage = merged.size();
            keysSkipped += stats.keysSkipped;
            duplicateWarnings += stats.duplicateKeyWarnings;
            LOGGER.info(
                    "{} {} - {} keys from {} lang file stacks ({} pack layers, {})",
                    ExportLog.LANG,
                    langCode,
                    merged.size(),
                    stacks.size(),
                    stats.resourceLayersRead,
                    mode);
        }

        if (onlyKeys != null) {
            LOGGER.info(
                    "{} closure key filter: {} requested, ~{} keys per language file, {} entries skipped while scanning",
                    ExportLog.LANG,
                    onlyKeys.size(),
                    keysPerLanguage,
                    keysSkipped);
        }
        if (duplicateWarnings > ExportLog.DETAIL_FAILURE_LIMIT) {
            LOGGER.warn(
                    "{} {} duplicate-key warnings while merging (first {} at DEBUG)",
                    ExportLog.LANG,
                    duplicateWarnings,
                    ExportLog.DETAIL_FAILURE_LIMIT);
        }

        return new Result(
                languagesWritten,
                totalBytes,
                duplicateWarnings,
                onlyKeys != null ? onlyKeys.size() : 0,
                keysSkipped,
                keysPerLanguage);
    }

    static boolean matchesLangPath(ResourceLocation location, String langFile) {
        String path = location.getPath();
        return path.equals(langFile) || path.equals("lang/" + langFile) || path.endsWith("/" + langFile);
    }

    static Map<ResourceLocation, List<Resource>> collectLangStacksForNamespaces(
            Minecraft client, String langFile, Set<String> onlyNamespaces) {
        return collectLangStacks(client, langFile, onlyNamespaces);
    }

    /**
     * Key-level merge of all pack layers (same order as {@code ClientLanguage.appendFrom}).
     */
    static void mergeLangStacksInto(
            Map<String, String> merged,
            Map<ResourceLocation, List<Resource>> stacks,
            Set<String> onlyKeys,
            MergeStats stats) {
        Map<String, ResourceLocation> keyOrigin = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<Resource>> stackEntry : stacks.entrySet()) {
            ResourceLocation location = stackEntry.getKey();
            List<Resource> layers = stackEntry.getValue();
            if (layers == null || layers.isEmpty()) {
                continue;
            }
            for (Resource resource : layers) {
                stats.resourceLayersRead++;
                try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : object.entrySet()) {
                        String key = entry.getKey();
                        if (!shouldMergeLangKey(key, onlyKeys)) {
                            stats.keysSkipped++;
                            continue;
                        }
                        String value = entry.getValue().getAsString();
                        ResourceLocation previous = keyOrigin.get(key);
                        if (previous != null && !previous.equals(location)) {
                            stats.duplicateKeyWarnings++;
                            ExportLog.detailFailure(
                                    LOGGER,
                                    stats.duplicateKeyWarnings,
                                    "{} duplicate key '{}' from {} (was {})",
                                    ExportLog.LANG,
                                    key,
                                    location,
                                    previous);
                        }
                        merged.put(key, value);
                        keyOrigin.put(key, location);
                    }
                } catch (Exception e) {
                    LOGGER.warn("{} failed to read {}: {}", ExportLog.LANG, location, e.getMessage());
                }
            }
        }
    }

    private static Map<ResourceLocation, List<Resource>> collectLangStacks(
            Minecraft client,
            String langFile,
            Set<String> onlyNamespaces) {
        Predicate<ResourceLocation> filter = location -> matchesLangPath(location, langFile)
                && !ResourceExportFilter.isExcluded(location)
                && (onlyNamespaces == null || onlyNamespaces.contains(location.getNamespace()));

        Map<ResourceLocation, List<Resource>> stacks = new LinkedHashMap<>();
        appendLangStacks(stacks, client.getResourceManager(), filter);
        var server = client.getSingleplayerServer();
        if (server != null) {
            appendLangStacks(stacks, server.getResourceManager(), filter);
        }
        return stacks;
    }

    private static void appendLangStacks(
            Map<ResourceLocation, List<Resource>> into,
            ResourceManager resourceManager,
            Predicate<ResourceLocation> filter) {
        for (var entry : resourceManager.listResourceStacks("lang", filter).entrySet()) {
            into.compute(
                    entry.getKey(),
                    (id, existing) -> {
                        if (existing == null || existing.isEmpty()) {
                            return new ArrayList<>(entry.getValue());
                        }
                        var combined = new ArrayList<>(existing);
                        combined.addAll(entry.getValue());
                        return combined;
                    });
        }
    }

    private static void logLangPathProbe(Minecraft client, String langFile) {
        int shown = 0;
        StringBuilder sample = new StringBuilder();
        for (ResourceLocation location :
                client.getResourceManager().listResourceStacks("lang", loc -> matchesLangPath(loc, langFile)).keySet()) {
            if (shown++ >= 5) {
                break;
            }
            if (shown > 1) {
                sample.append(", ");
            }
            sample.append(location);
        }
        if (shown > 0) {
            LOGGER.warn(
                    "{} client has {} lang file stack(s) for {} but none passed namespace filter; sample: {}",
                    ExportLog.LANG,
                    shown,
                    langFile,
                    sample);
        } else {
            LOGGER.warn(
                    "{} client ResourceManager has no resources under lang/ for {} (assets not loaded?)",
                    ExportLog.LANG,
                    langFile);
        }
    }
}
