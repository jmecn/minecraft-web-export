package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

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
import java.util.logging.Logger;

public final class LangMergerExporter {

    private static final Logger LOGGER = Logger.getLogger(LangMergerExporter.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
                LOGGER.warning("[lang] " + langCode + " - no mod lang files matched (namespaces="
                        + (onlyNamespaces == null ? "all" : onlyNamespaces) + ")");
                logLangPathProbe(client, langFile);
            }

            for (Map.Entry<ResourceLocation, Resource> hit : hits.entrySet()) {
                try (var reader = new InputStreamReader(hit.getValue().open(), StandardCharsets.UTF_8)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : object.entrySet()) {
                        String key = entry.getKey();
                        if (onlyKeys != null && !onlyKeys.contains(key)) {
                            keysSkipped++;
                            continue;
                        }
                        String value = entry.getValue().getAsString();
                        if (merged.containsKey(key)) {
                            duplicateWarnings++;
                            if (duplicateWarnings <= 20) {
                                LOGGER.warning("[lang] duplicate key '" + key + "' from " + hit.getKey());
                            }
                        }
                        merged.put(key, value);
                    }
                } catch (Exception e) {
                    LOGGER.warning("[lang] failed to read " + hit.getKey() + ": " + e.getMessage());
                }
            }

            if (merged.isEmpty()) {
                LOGGER.warning("[lang] " + langCode + " - 0 keys after merge (" + mode + ", " + hits.size() + " mod files read)");
                continue;
            }

            Path out = langRoot.resolve(langFile);
            String json = GSON.toJson(merged);
            Files.writeString(out, json);
            languagesWritten++;
            totalBytes += json.length();
            keysPerLanguage = merged.size();
            LOGGER.info("[lang] " + langCode + " - " + merged.size() + " keys from " + hits.size() + " mod files (" + mode + ")");
        }

        if (onlyKeys != null) {
            LOGGER.info("[lang] closure key filter: " + onlyKeys.size()
                    + " requested, ~" + keysPerLanguage + " keys per language file, "
                    + keysSkipped + " entries skipped while scanning");
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
            LOGGER.warning("[lang] client has " + shown + " lang file(s) for " + langFile
                    + " but none passed namespace filter; sample: " + sample);
        } else {
            LOGGER.warning("[lang] client ResourceManager has no resources under lang/ for "
                    + langFile + " (assets not loaded?)");
        }
    }
}
