package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Precomputes {@code items-search/<locale>.json} for substring item filtering in the site shell.
 */
public final class ItemsSearchIndexExporter {

    private static final Logger LOGGER = LogManager.getLogger(ItemsSearchIndexExporter.class);
    private static final Gson GSON = ExportGson.GSON;
    private static final String ITEMS_SEARCH_DIR = "items-search";
    private static final int PROGRESS_EVERY = 5000;

    private ItemsSearchIndexExporter() {
    }

    public record Result(int localeCount, int itemCount, List<String> locales) {
        static final Result EMPTY = new Result(0, 0, List.of());
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipItemsSearchExport");
    }

    public static Result export(Path outputDir, List<String> languages) throws IOException {
        Path bundleRoot = EmiBundlePaths.resolve(outputDir, "");
        List<String> itemIds = readItemIds(bundleRoot);
        List<String> locales = resolveLocales(bundleRoot, languages);
        if (itemIds.isEmpty() || locales.isEmpty()) {
            LOGGER.warn(
                    "{} skipped: {} items, {} locales",
                    ExportLog.ITEMS_SEARCH,
                    itemIds.size(),
                    locales.size());
            return new Result(0, itemIds.size(), List.of());
        }

        Path searchRoot = bundleRoot.resolve(ITEMS_SEARCH_DIR);
        Files.createDirectories(searchRoot);

        Map<String, String> enUs = readLangTable(bundleRoot, EmiBundlePaths.DEFAULT_LANGUAGE);
        Map<String, String> nameKeysByRegistryId = ItemNameKeysExporter.readNameKeys(outputDir);
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            LOGGER.info(
                    "{} {}: building {} items ...",
                    ExportLog.ITEMS_SEARCH,
                    normalized,
                    itemIds.size());
            long startedAt = System.currentTimeMillis();
            Map<String, String> current = readLangTable(bundleRoot, normalized);
            Map<String, String> fallback = EmiBundlePaths.DEFAULT_LANGUAGE.equals(normalized)
                    ? Map.of()
                    : enUs;
            RegistryLabelResolver currentResolver = new RegistryLabelResolver(current, fallback, nameKeysByRegistryId);
            RegistryLabelResolver enResolver = isChineseLocale(normalized) && !EmiBundlePaths.DEFAULT_LANGUAGE.equals(normalized)
                    ? new RegistryLabelResolver(enUs, Map.of(), nameKeysByRegistryId)
                    : null;

            JsonArray items = new JsonArray();
            for (int i = 0; i < itemIds.size(); i++) {
                String id = itemIds.get(i);
                JsonObject row = new JsonObject();
                row.addProperty("id", id);
                row.addProperty("haystack", buildHaystack(id, normalized, currentResolver, enResolver));
                items.add(row);
                int n = i + 1;
                if (n % PROGRESS_EVERY == 0) {
                    LOGGER.info(
                            "{} {}: {}/{} ({} ms)",
                            ExportLog.ITEMS_SEARCH,
                            normalized,
                            n,
                            itemIds.size(),
                            System.currentTimeMillis() - startedAt);
                }
            }
            LOGGER.info(
                    "{} {}: {} done ({} ms)",
                    ExportLog.ITEMS_SEARCH,
                    normalized,
                    itemIds.size(),
                    System.currentTimeMillis() - startedAt);

            JsonObject payload = new JsonObject();
            payload.addProperty("schema", 1);
            payload.addProperty("locale", normalized);
            payload.addProperty("itemCount", itemIds.size());
            payload.add("items", items);

            Path out = searchRoot.resolve(normalized + ".json");
            LOGGER.info("{} {}: writing {} ...", ExportLog.ITEMS_SEARCH, normalized, out);
            Files.writeString(out, GSON.toJson(payload) + "\n", StandardCharsets.UTF_8);
            writtenLocales.add(normalized);
            LOGGER.info(
                    "{} {}: {} items",
                    ExportLog.ITEMS_SEARCH,
                    normalized,
                    itemIds.size());
        }

        return new Result(writtenLocales.size(), itemIds.size(), List.copyOf(writtenLocales));
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return EmiBundlePaths.DEFAULT_LANGUAGE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    static boolean isChineseLocale(String locale) {
        String normalized = normalizeLocale(locale);
        return normalized.startsWith("zh_") || "zh".equals(normalized);
    }

    static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§.", "");
    }

    static String buildHaystack(
            String id,
            String locale,
            RegistryLabelResolver currentResolver,
            RegistryLabelResolver enResolver) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> parts = new ArrayList<>();

        appendToken(seen, parts, id);

        String nameCurrent = currentResolver.translateRegistry(id);
        appendToken(seen, parts, nameCurrent);

        if (isChineseLocale(locale)) {
            if (enResolver != null) {
                String nameEn = enResolver.translateRegistry(id);
                appendToken(seen, parts, nameEn);
            }
            for (String py : SearchPinyin.tokensForLabel(nameCurrent)) {
                appendToken(seen, parts, py);
            }
        }

        return String.join(" ", parts);
    }

    private static void appendToken(Set<String> seen, List<String> parts, String raw) {
        String token = stripFormatting(raw).toLowerCase(Locale.ROOT).trim();
        if (token.isEmpty() || !seen.add(token)) {
            return;
        }
        parts.add(token);
    }

    private static List<String> readItemIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(EmiBundlePaths.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return List.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            if ("schema".equals(entry.getKey()) || !entry.getValue().isJsonArray()) {
                continue;
            }
            String namespace = entry.getKey();
            for (JsonElement pathEl : entry.getValue().getAsJsonArray()) {
                if (!pathEl.isJsonPrimitive()) {
                    continue;
                }
                String path = pathEl.getAsString();
                if (path == null || path.isEmpty()) {
                    continue;
                }
                ids.add(path.contains(":") ? path : namespace + ":" + path);
            }
        }
        return List.copyOf(ids);
    }

    private static List<String> resolveLocales(Path bundleRoot, List<String> languages) throws IOException {
        if (languages != null && !languages.isEmpty()) {
            return languages.stream().map(ItemsSearchIndexExporter::normalizeLocale).distinct().sorted().toList();
        }
        Path langDir = bundleRoot.resolve(EmiBundlePaths.LANG_DIR);
        if (!Files.isDirectory(langDir)) {
            return List.of(EmiBundlePaths.DEFAULT_LANGUAGE);
        }
        try (var stream = Files.list(langDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> normalizeLocale(name.substring(0, name.length() - 5)))
                    .sorted()
                    .toList();
        }
    }

    private static Map<String, String> readLangTable(Path bundleRoot, String locale) throws IOException {
        Path langPath = bundleRoot.resolve(EmiBundlePaths.LANG_DIR).resolve(locale + ".json");
        if (!Files.isRegularFile(langPath)) {
            return Map.of();
        }
        JsonObject object = JsonParser.parseString(Files.readString(langPath)).getAsJsonObject();
        Map<String, String> table = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                table.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return table;
    }
}
