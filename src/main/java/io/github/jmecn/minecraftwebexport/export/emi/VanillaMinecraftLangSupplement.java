package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Closure lang merge only sees modpack {@code assets/minecraft/lang} overrides (~100 keys).
 * Pull missing {@code item|block|fluid.minecraft.*} from the vanilla resource pack (and en_us fallback).
 */
final class VanillaMinecraftLangSupplement {

    private static final Logger LOGGER = LogManager.getLogger(VanillaMinecraftLangSupplement.class);

    private VanillaMinecraftLangSupplement() {
    }

    static boolean isMinecraftRegistryLangKey(String key) {
        return key != null
                && (key.startsWith("item.minecraft.")
                        || key.startsWith("block.minecraft.")
                        || key.startsWith("fluid.minecraft."));
    }

    static int supplement(Map<String, String> merged, Minecraft client, String langCode, Set<String> onlyKeys) {
        if (onlyKeys == null || onlyKeys.isEmpty()) {
            return 0;
        }
        Set<String> missing = new TreeSet<>();
        for (String key : onlyKeys) {
            if (isMinecraftRegistryLangKey(key) && !merged.containsKey(key)) {
                missing.add(key);
            }
        }
        if (missing.isEmpty()) {
            return 0;
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
            LOGGER.info(
                    "{} {} - {} minecraft registry lang keys still missing after vanilla supplement (e.g. {})",
                    ExportLog.LANG,
                    langCode,
                    missing.size(),
                    sample);
        } else if (added > 0) {
            LOGGER.info(
                    "{} {} - supplemented {} minecraft registry lang keys from vanilla pack",
                    ExportLog.LANG,
                    langCode,
                    added);
        }
        return added;
    }

    private static int copyMissingFromMinecraftPack(
            Map<String, String> merged, Minecraft client, String langFile, Set<String> missing) {
        Map<ResourceLocation, Resource> hits =
                LangMergerExporter.collectLangHitsForNamespaces(client, langFile, Set.of("minecraft"));
        if (hits.isEmpty()) {
            return 0;
        }
        int added = 0;
        Set<String> found = new TreeSet<>();
        for (Resource resource : hits.values()) {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : missing) {
                    if (merged.containsKey(key) || !object.has(key)) {
                        continue;
                    }
                    merged.put(key, object.get(key).getAsString());
                    found.add(key);
                    added++;
                }
            } catch (Exception e) {
                LOGGER.warn("{} failed to read vanilla minecraft lang {}: {}", ExportLog.LANG, langFile, e.getMessage());
            }
        }
        missing.removeAll(found);
        return added;
    }
}
