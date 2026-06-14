package io.github.jmecn.minecraftwebexport.emi.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.lang.LangMergeResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class Merger {

    private Merger() {
    }


    static final class MergeStats {
        int keysSkipped;
        int duplicateKeyWarnings;
        int resourceLayersRead;
    }

    public static boolean isEnabled() {
        return !MweConfig.skipLangExport();
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

    private static boolean isEmiCategoryLangKey(String key) {
        return key.startsWith("emi.category.");
    }

    public static LangMergeResult exportEmiLang(
            Path outputDir, Minecraft client, Set<String> onlyKeys, Hints hints, ExportWriteQueue writes)
            throws IOException {
        return exportTo(EmiPaths.resolve(outputDir, Constants.LANG_DIR), client, null, onlyKeys, hints, writes);
    }

    private static LangMergeResult exportTo(
            Path langRoot,
            Minecraft client,
            Set<String> onlyNamespaces,
            Set<String> onlyKeys,
            Hints hints,
            ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        Files.createDirectories(langRoot);

        Set<String> languages = Languages.resolve(hints);
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
                MweMod.LOGGER.warn(
                        "{} {} - no mod lang files matched (namespaces={})",
                        Log.LANG,
                        langCode,
                        onlyNamespaces == null ? "all" : onlyNamespaces);
                logLangPathProbe(client, langFile);
            } else {
                mergeLangStacksInto(merged, stacks, onlyKeys, stats);
            }

            if (onlyKeys != null) {
                supplementVanillaKeys(merged, client, langCode, onlyKeys);
            }

            if (merged.isEmpty()) {
                MweMod.LOGGER.warn(
                        "{} {} - 0 keys after merge ({}, {} lang file stacks, {} pack layers)",
                        Log.LANG,
                        langCode,
                        mode,
                        stacks.size(),
                        stats.resourceLayersRead);
                continue;
            }

            Path out = langRoot.resolve(langFile);
            writes.submitJson(out, merged);
            languagesWritten++;
            totalBytes += JsonIO.toUtf8Bytes(merged).length;
            keysPerLanguage = merged.size();
            keysSkipped += stats.keysSkipped;
            duplicateWarnings += stats.duplicateKeyWarnings;
            MweMod.LOGGER.info(
                    "{} {} - {} keys from {} lang file stacks ({} pack layers, {})",
                    Log.LANG,
                    langCode,
                    merged.size(),
                    stacks.size(),
                    stats.resourceLayersRead,
                    mode);
        }

        if (onlyKeys != null) {
            MweMod.LOGGER.info(
                    "{} closure key filter: {} requested, ~{} keys per language file, {} entries skipped while scanning",
                    Log.LANG,
                    onlyKeys.size(),
                    keysPerLanguage,
                    keysSkipped);
        }
        if (duplicateWarnings > Log.DETAIL_FAILURE_LIMIT) {
            MweMod.LOGGER.warn(
                    "{} {} duplicate-key warnings while merging (first {} at DEBUG)",
                    Log.LANG,
                    duplicateWarnings,
                    Log.DETAIL_FAILURE_LIMIT);
        }

        return new LangMergeResult(
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
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                        String key = entry.getKey();
                        if (!shouldMergeLangKey(key, onlyKeys)) {
                            stats.keysSkipped++;
                            continue;
                        }
                        String value = entry.getValue().getAsString();
                        ResourceLocation previous = keyOrigin.get(key);
                        if (previous != null && !previous.equals(location)) {
                            stats.duplicateKeyWarnings++;
                            Log.detailFailure(stats.duplicateKeyWarnings,
                                    "{} duplicate key '{}' from {} (was {})",
                                    Log.LANG,
                                    key,
                                    location,
                                    previous);
                        }
                        merged.put(key, value);
                        keyOrigin.put(key, location);
                    }
                } catch (Exception e) {
                    MweMod.LOGGER.warn("{} failed to read {}: {}", Log.LANG, location, e.getMessage());
                }
            }
        }
    }

    private static Map<ResourceLocation, List<Resource>> collectLangStacks(
            Minecraft client,
            String langFile,
            Set<String> onlyNamespaces) {
        Predicate<ResourceLocation> filter = location -> matchesLangPath(location, langFile)
                && !isExcludedNamespace(location)
                && (onlyNamespaces == null || onlyNamespaces.contains(location.getNamespace()));

        Map<ResourceLocation, List<Resource>> stacks = new LinkedHashMap<>();
        appendLangStacks(stacks, client.getResourceManager(), filter);
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null) {
            appendLangStacks(stacks, server.getResourceManager(), filter);
        }
        return stacks;
    }

    private static void appendLangStacks(
            Map<ResourceLocation, List<Resource>> into,
            ResourceManager resourceManager,
            Predicate<ResourceLocation> filter) {
        for (Map.Entry<ResourceLocation, List<Resource>> entry : resourceManager.listResourceStacks("lang", filter).entrySet()) {
            into.compute(
                    entry.getKey(),
                    (id, existing) -> {
                        if (existing == null || existing.isEmpty()) {
                            return new ArrayList<>(entry.getValue());
                        }
                        ArrayList<Resource> combined = new ArrayList<>(existing);
                        combined.addAll(entry.getValue());
                        return combined;
                    });
        }
    }

    static boolean isMinecraftRegistryLangKey(String key) {
        return key != null
                && (key.startsWith("item.minecraft.")
                        || key.startsWith("block.minecraft.")
                        || key.startsWith("fluid.minecraft."));
    }

    static Set<String> excludedNamespaces() {
        return mergeExcludedNamespaces(MweConfig.excludedNamespaces());
    }

    static Set<String> mergeExcludedNamespaces(String extra) {
        String trimmed = extra == null ? "" : extra.trim();
        if (trimmed.isEmpty()) {
            return Constants.DEFAULT_EXCLUDED_NAMESPACES;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(Constants.DEFAULT_EXCLUDED_NAMESPACES);
        Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(merged::add);
        return Set.copyOf(merged);
    }

    private static boolean isExcludedNamespace(ResourceLocation id) {
        return excludedNamespaces().contains(id.getNamespace());
    }

    private static void supplementVanillaKeys(
            Map<String, String> merged, Minecraft client, String langCode, Set<String> onlyKeys) {
        if (onlyKeys == null || onlyKeys.isEmpty()) {
            return;
        }
        Set<String> missing = new TreeSet<>();
        for (String key : onlyKeys) {
            if (isMinecraftRegistryLangKey(key) && !merged.containsKey(key)) {
                missing.add(key);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        List<String> locales = new ArrayList<>();
        locales.add(langCode);
        if (!"en_us".equals(langCode)) {
            locales.add("en_us");
        }

        int added = 0;
        for (String locale : locales) {
            if (missing.isEmpty()) {
                break;
            }
            added += copyMissingFromMinecraftPack(merged, client, locale + ".json", missing);
        }

        if (!missing.isEmpty()) {
            String sample = missing.stream().limit(5).reduce((a, b) -> a + ", " + b).orElse("");
            MweMod.LOGGER.info(
                    "{} {} - {} minecraft registry lang keys still missing after vanilla supplement (e.g. {})",
                    Log.LANG,
                    langCode,
                    missing.size(),
                    sample);
        } else if (added > 0) {
            MweMod.LOGGER.info(
                    "{} {} - supplemented {} minecraft registry lang keys from vanilla pack",
                    Log.LANG,
                    langCode,
                    added);
        }
    }

    private static int copyMissingFromMinecraftPack(
            Map<String, String> merged, Minecraft client, String langFile, Set<String> missing) {
        Map<ResourceLocation, List<Resource>> stacks =
                collectLangStacksForNamespaces(client, langFile, Set.of("minecraft"));
        if (stacks.isEmpty()) {
            return 0;
        }
        Map<String, String> minecraftMerged = new TreeMap<>();
        mergeLangStacksInto(minecraftMerged, stacks, null, new MergeStats());
        int added = 0;
        Set<String> found = new TreeSet<>();
        for (String key : missing) {
            if (merged.containsKey(key)) {
                continue;
            }
            String value = minecraftMerged.get(key);
            if (value == null) {
                continue;
            }
            merged.put(key, value);
            found.add(key);
            added++;
        }
        missing.removeAll(found);
        return added;
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
            MweMod.LOGGER.warn(
                    "{} client has {} lang file stack(s) for {} but none passed namespace filter; sample: {}",
                    Log.LANG,
                    shown,
                    langFile,
                    sample);
        } else {
            MweMod.LOGGER.warn(
                    "{} client ResourceManager has no resources under lang/ for {} (assets not loaded?)",
                    Log.LANG,
                    langFile);
        }
    }
}
