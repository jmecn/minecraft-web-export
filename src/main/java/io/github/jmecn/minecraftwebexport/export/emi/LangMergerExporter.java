package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

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
        return isGtceuTranslationKey(key) || isEmiCategoryLangKey(key);
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

    public static Result exportEmiLang(Path outputDir, Minecraft client, Set<String> onlyKeys) throws IOException {
        return exportTo(EmiBundlePaths.resolve(outputDir, EmiBundlePaths.LANG_DIR), client, null, onlyKeys);
    }

    public static Result exportTo(Path langRoot, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
            throws IOException {
        Files.createDirectories(langRoot);

        Set<String> languages = MinecraftWebExportLanguages.resolve();
        if (languages == null) {
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
            Map<ResourceLocation, Resource> hits = collectLangHits(client, langFile, onlyNamespaces);

            if (hits.isEmpty()) {
                LOGGER.warn(
                        "{} {} - no mod lang files matched (namespaces={})",
                        ExportLog.LANG,
                        langCode,
                        onlyNamespaces == null ? "all" : onlyNamespaces);
                logLangPathProbe(client, langFile);
            }

            for (Map.Entry<ResourceLocation, Resource> hit : hits.entrySet()) {
                try (var reader = new InputStreamReader(hit.getValue().open(), StandardCharsets.UTF_8)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : object.entrySet()) {
                        String key = entry.getKey();
                        if (!shouldMergeLangKey(key, onlyKeys)) {
                            keysSkipped++;
                            continue;
                        }
                        String value = entry.getValue().getAsString();
                        if (merged.containsKey(key)) {
                            duplicateWarnings++;
                            ExportLog.detailFailure(
                                    LOGGER,
                                    duplicateWarnings,
                                    "{} duplicate key '{}' from {}",
                                    ExportLog.LANG,
                                    key,
                                    hit.getKey());
                        }
                        merged.put(key, value);
                    }
                } catch (Exception e) {
                    LOGGER.warn("{} failed to read {}: {}", ExportLog.LANG, hit.getKey(), e.getMessage());
                }
            }

            if (onlyKeys != null) {
                VanillaMinecraftLangSupplement.supplement(merged, client, langCode, onlyKeys);
            }

            if (merged.isEmpty()) {
                LOGGER.warn(
                        "{} {} - 0 keys after merge ({}, {} mod files read)",
                        ExportLog.LANG,
                        langCode,
                        mode,
                        hits.size());
                continue;
            }

            Path out = langRoot.resolve(langFile);
            String json = GSON.toJson(merged);
            Files.writeString(out, json);
            languagesWritten++;
            totalBytes += json.length();
            keysPerLanguage = merged.size();
            LOGGER.info(
                    "{} {} - {} keys from {} mod files ({})",
                    ExportLog.LANG,
                    langCode,
                    merged.size(),
                    hits.size(),
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

    static Map<ResourceLocation, Resource> collectLangHitsForNamespaces(
            Minecraft client, String langFile, Set<String> onlyNamespaces) {
        return collectLangHits(client, langFile, onlyNamespaces);
    }

    private static Map<ResourceLocation, Resource> collectLangHits(
            Minecraft client,
            String langFile,
            Set<String> onlyNamespaces) {
        Predicate<ResourceLocation> filter = location -> matchesLangPath(location, langFile)
                && !ResourceExportFilter.isExcluded(location)
                && (onlyNamespaces == null || onlyNamespaces.contains(location.getNamespace()));

        Map<ResourceLocation, Resource> hits = new LinkedHashMap<>();
        mergeLangHits(hits, client.getResourceManager(), filter);
        var server = client.getSingleplayerServer();
        if (server != null) {
            mergeLangHits(hits, server.getResourceManager(), filter);
        }
        return hits;
    }

    private static void mergeLangHits(
            Map<ResourceLocation, Resource> into,
            ResourceManager resourceManager,
            Predicate<ResourceLocation> filter) {
        for (var entry : resourceManager.listResources("lang", filter).entrySet()) {
            into.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static void logLangPathProbe(Minecraft client, String langFile) {
        int shown = 0;
        StringBuilder sample = new StringBuilder();
        for (ResourceLocation location : client.getResourceManager().listResources("lang", loc -> matchesLangPath(loc, langFile)).keySet()) {
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
                    "{} client has {} lang file(s) for {} but none passed namespace filter; sample: {}",
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
