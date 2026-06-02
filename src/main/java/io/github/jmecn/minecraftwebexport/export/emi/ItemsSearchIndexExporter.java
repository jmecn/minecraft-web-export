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
 * Precomputes {@code items-lang/<locale>.json} for registry labels and substring item filtering.
 */
public final class ItemsSearchIndexExporter {

    /** Top-level key in {@code items/index.json} listing registry ids referenced as fluids in recipes. */
    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    private static final Logger LOGGER = LogManager.getLogger(ItemsSearchIndexExporter.class);
    private static final Gson GSON = ExportGson.GSON;
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
        return export(outputDir, languages, false);
    }

    public static Result export(Path outputDir, List<String> languages, boolean readComposeLang) throws IOException {
        Path bundleRoot = EmiBundlePaths.resolve(outputDir, "");
        List<String> itemIds = readItemIds(bundleRoot);
        Set<String> fluidRegistryIds = readFluidRegistryIds(bundleRoot);
        List<String> locales = resolveLocales(bundleRoot, languages);
        if (itemIds.isEmpty() || locales.isEmpty()) {
            LOGGER.warn(
                    "{} skipped: {} items, {} locales",
                    ExportLog.ITEMS_LANG,
                    itemIds.size(),
                    locales.size());
            return new Result(0, itemIds.size(), List.of());
        }

        Path searchRoot = bundleRoot.resolve(EmiBundlePaths.ITEMS_LANG_DIR);
        Files.createDirectories(searchRoot);

        Map<String, String> enUs = readLangTable(bundleRoot, EmiBundlePaths.DEFAULT_LANGUAGE, readComposeLang);
        Map<String, String> nameKeysByRegistryId = ItemNameKeysExporter.readNameKeys(outputDir);
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            LOGGER.info(
                    "{} {}: building {} items ...",
                    ExportLog.ITEMS_LANG,
                    normalized,
                    itemIds.size());
            long startedAt = System.currentTimeMillis();
            Map<String, String> current = readLangTable(bundleRoot, normalized, readComposeLang);
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
                String kind = resolveRegistryKind(id, fluidRegistryIds, currentResolver);
                String label = currentResolver.translateRegistry(id, kind);
                JsonObject row = new JsonObject();
                row.addProperty("id", id);
                row.addProperty("label", label);
                row.addProperty(
                        "haystack",
                        buildHaystack(id, kind, normalized, currentResolver, enResolver));
                items.add(row);
                int n = i + 1;
                if (n % PROGRESS_EVERY == 0) {
                    LOGGER.info(
                            "{} {}: {}/{} ({} ms)",
                            ExportLog.ITEMS_LANG,
                            normalized,
                            n,
                            itemIds.size(),
                            System.currentTimeMillis() - startedAt);
                }
            }
            LOGGER.info(
                    "{} {}: {} done ({} ms)",
                    ExportLog.ITEMS_LANG,
                    normalized,
                    itemIds.size(),
                    System.currentTimeMillis() - startedAt);

            JsonObject payload = new JsonObject();
            payload.addProperty("schema", 2);
            payload.addProperty("locale", normalized);
            payload.addProperty("itemCount", itemIds.size());
            payload.add("items", items);

            Path out = searchRoot.resolve(normalized + ".json");
            LOGGER.info("{} {}: writing {} ...", ExportLog.ITEMS_LANG, normalized, out);
            Files.writeString(out, GSON.toJson(payload) + "\n", StandardCharsets.UTF_8);
            writtenLocales.add(normalized);
            LOGGER.info(
                    "{} {}: {} items",
                    ExportLog.ITEMS_LANG,
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
            String kind,
            String locale,
            RegistryLabelResolver currentResolver,
            RegistryLabelResolver enResolver) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> parts = new ArrayList<>();

        appendToken(seen, parts, id);

        String nameCurrent = currentResolver.translateRegistry(id, kind);
        appendToken(seen, parts, nameCurrent);

        if (isChineseLocale(locale)) {
            if (enResolver != null) {
                String nameEn = enResolver.translateRegistry(id, kind);
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

    /**
     * {@code item} unless listed in {@link #FLUID_REGISTRY_IDS_KEY}, or heuristically a GTCEu/TFG fluid
     * when the index predates fluid tracking.
     */
    static String resolveRegistryKind(
            String registryId,
            Set<String> fluidRegistryIds,
            RegistryLabelResolver resolver) {
        if (fluidRegistryIds != null && fluidRegistryIds.contains(registryId)) {
            return "fluid";
        }
        if (fluidRegistryIds == null || fluidRegistryIds.isEmpty()) {
            return inferFluidKindWhenIndexMissing(registryId, resolver);
        }
        return "item";
    }

    private static String inferFluidKindWhenIndexMissing(
            String registryId, RegistryLabelResolver resolver) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        String namespace = RegistryLabelResolver.registryNamespace(bare);
        if (!RegistryLabelResolver.COMPOSED_FIRST_NAMESPACES.contains(namespace)) {
            return "item";
        }
        int colon = bare.indexOf(':');
        if (colon <= 0 || colon >= bare.length() - 1) {
            return "item";
        }
        String path = bare.substring(colon + 1);
        if (path.endsWith("_bucket")) {
            return "item";
        }
        String asItem = resolver.translateRegistry(registryId, "item");
        if (!asItem.equals(bare)) {
            return "item";
        }
        String asFluid = resolver.translateRegistry(registryId, "fluid");
        return asFluid.equals(bare) ? "item" : "fluid";
    }

    static Set<String> readFluidRegistryIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(EmiBundlePaths.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        if (!index.has(FLUID_REGISTRY_IDS_KEY) || !index.get(FLUID_REGISTRY_IDS_KEY).isJsonArray()) {
            return Set.of();
        }
        Set<String> ids = new TreeSet<>();
        for (JsonElement element : index.getAsJsonArray(FLUID_REGISTRY_IDS_KEY)) {
            if (element.isJsonPrimitive()) {
                String id = element.getAsString();
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return Set.copyOf(ids);
    }

    private static List<String> readItemIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(EmiBundlePaths.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return List.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String key = entry.getKey();
            if ("schema".equals(key)
                    || FLUID_REGISTRY_IDS_KEY.equals(key)
                    || !entry.getValue().isJsonArray()) {
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

    private static Map<String, String> readLangTable(Path bundleRoot, String locale, boolean composeLang) throws IOException {
        String dir = composeLang ? EmiBundlePaths.COMPOSE_LANG_DIR : EmiBundlePaths.LANG_DIR;
        Path langPath = bundleRoot.resolve(dir).resolve(locale + ".json");
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
