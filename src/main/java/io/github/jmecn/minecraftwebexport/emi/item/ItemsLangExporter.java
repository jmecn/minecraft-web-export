package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryResolver;
import io.github.jmecn.minecraftwebexport.emi.lang.SearchPinyin;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemsLangExportResult;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.ItemsLang;
import io.github.jmecn.minecraftwebexport.model.item.ItemsLangEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class ItemsLangExporter {

    private ItemsLangExporter() {
    }

    public static boolean isEnabled() {
        return !MweConfig.skipItemsSearchExport();
    }

    public static ItemsLangExportResult export(Path outputDir, List<String> languages) throws IOException {
        Path bundleRoot = EmiPaths.resolve(outputDir, "");
        return export(outputDir, languages, readItemIds(bundleRoot), readFluidRegistryIds(bundleRoot));
    }

    public static ItemsLangExportResult export(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds) throws IOException {
        try (ExportWriteQueue writes = new ExportWriteQueue()) {
            ItemsLangExportResult result = export(outputDir, languages, itemIds, fluidRegistryIds, writes);
            writes.awaitIdle();
            return result;
        }
    }

    public static ItemsLangExportResult export(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds,
            ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        Path bundleRoot = EmiPaths.resolve(outputDir, "");
        List<String> sortedItemIds = itemIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .distinct()
                .toList();
        Set<String> fluids = fluidRegistryIds == null ? Set.of() : fluidRegistryIds;
        List<String> locales = resolveLocales(bundleRoot, languages);
        if (sortedItemIds.isEmpty() || locales.isEmpty()) {
            MweMod.LOGGER.warn(
                    "{} skipped: {} items, {} locales",
                    Log.ITEMS_LANG,
                    sortedItemIds.size(),
                    locales.size());
            return ItemsLangExportResult.EMPTY;
        }

        Path searchRoot = bundleRoot.resolve(Constants.ITEMS_LANG_DIR);
        Files.createDirectories(searchRoot);

        Map<String, String> enUs = readLangTable(bundleRoot, Constants.DEFAULT_LANGUAGE);
        Map<String, String> nameKeysByRegistryId = NameKeysExporter.readNameKeys(outputDir);
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            MweMod.LOGGER.info(
                    "{} {}: building {} items ...",
                    Log.ITEMS_LANG,
                    normalized,
                    sortedItemIds.size());
            long startedAt = System.currentTimeMillis();
            Map<String, String> current = readLangTable(bundleRoot, normalized);
            Map<String, String> fallback = Constants.DEFAULT_LANGUAGE.equals(normalized)
                    ? Map.of()
                    : enUs;
            RegistryResolver currentResolver = new RegistryResolver(current, fallback, nameKeysByRegistryId);
            RegistryResolver enResolver = isChineseLocale(normalized) && !Constants.DEFAULT_LANGUAGE.equals(normalized)
                    ? new RegistryResolver(enUs, Map.of(), nameKeysByRegistryId)
                    : null;

            List<ItemsLangEntry> entries = new ArrayList<>(sortedItemIds.size());
            for (int i = 0; i < sortedItemIds.size(); i++) {
                String id = sortedItemIds.get(i);
                String kind = resolveRegistryKind(id, fluids, currentResolver);
                String label = currentResolver.translateRegistry(id, kind);
                entries.add(new ItemsLangEntry(
                        id,
                        label,
                        buildHaystack(id, kind, normalized, currentResolver, enResolver)));
                int n = i + 1;
                if (n % Constants.SEARCH_INDEX_PROGRESS_EVERY == 0) {
                    MweMod.LOGGER.info(
                            "{} {}: {}/{} ({} ms)",
                            Log.ITEMS_LANG,
                            normalized,
                            n,
                            sortedItemIds.size(),
                            System.currentTimeMillis() - startedAt);
                }
            }
            MweMod.LOGGER.info(
                    "{} {}: {} done ({} ms)",
                    Log.ITEMS_LANG,
                    normalized,
                    sortedItemIds.size(),
                    System.currentTimeMillis() - startedAt);

            ItemsLang payload = ItemsLang.of(normalized, entries);
            Path out = searchRoot.resolve(normalized + ".json");
            MweMod.LOGGER.info("{} {}: writing {} ...", Log.ITEMS_LANG, normalized, out);
            writes.submitJsonLine(out, payload);
            writtenLocales.add(normalized);
            MweMod.LOGGER.info(
                    "{} {}: {} items",
                    Log.ITEMS_LANG,
                    normalized,
                    sortedItemIds.size());
        }

        return new ItemsLangExportResult(writtenLocales.size(), sortedItemIds.size(), List.copyOf(writtenLocales));
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return Constants.DEFAULT_LANGUAGE;
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
            RegistryResolver currentResolver,
            RegistryResolver enResolver) {
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

    static String resolveRegistryKind(
            String registryId,
            Set<String> fluidRegistryIds,
            RegistryResolver resolver) {
        if (fluidRegistryIds != null && fluidRegistryIds.contains(registryId)) {
            return "fluid";
        }
        if (fluidRegistryIds == null || fluidRegistryIds.isEmpty()) {
            return inferFluidKindWhenIndexMissing(registryId, resolver);
        }
        return "item";
    }

    private static String inferFluidKindWhenIndexMissing(
            String registryId, RegistryResolver resolver) {
        String bare = RegistryKeys.normalizeRegistryId(registryId);
        String namespace = RegistryResolver.registryNamespace(bare);
        if (!RegistryResolver.COMPOSED_FIRST_NAMESPACES.contains(namespace)) {
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

    public static Set<String> readFluidRegistryIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        return Set.copyOf(JsonIO.read(indexPath, ItemIndex.class).fluidRegistryIds());
    }

    private static List<String> readItemIds(Path bundleRoot) throws IOException {
        return List.copyOf(readIndexedItemIds(bundleRoot));
    }

    public static Set<String> readIndexedItemIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        ItemIndex index = JsonIO.read(indexPath, ItemIndex.class);
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, List<String>> entry : index.namespacePaths().entrySet()) {
            String namespace = entry.getKey();
            for (String path : entry.getValue()) {
                if (path == null || path.isEmpty()) {
                    continue;
                }
                ids.add(path.contains(":") ? path : namespace + ":" + path);
            }
        }
        return Set.copyOf(ids);
    }

    private static List<String> resolveLocales(Path bundleRoot, List<String> languages) throws IOException {
        if (languages != null && !languages.isEmpty()) {
            return languages.stream().map(ItemsLangExporter::normalizeLocale).distinct().sorted().toList();
        }
        Path langDir = bundleRoot.resolve(Constants.LANG_DIR);
        if (!Files.isDirectory(langDir)) {
            return List.of(Constants.DEFAULT_LANGUAGE);
        }
        try (Stream<Path> stream = Files.list(langDir)) {
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
        Path langPath = bundleRoot.resolve(Constants.LANG_DIR).resolve(locale + ".json");
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
