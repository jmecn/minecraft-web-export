package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryResolver;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
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
import java.util.TreeSet;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public final class ItemsLangExporter {

    private ItemsLangExporter() {
    }

    /**
     * Build and queue {@code items-lang/} search indexes using in-memory lang tables and name keys.
     */
    public static List<String> export(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds,
            Map<String, Map<String, String>> langTablesByLocale,
            Map<String, String> nameKeysByRegistryId,
            ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        Map<String, Map<String, String>> langTables =
                langTablesByLocale == null ? Map.of() : langTablesByLocale;
        Map<String, String> nameKeys = nameKeysByRegistryId == null ? Map.of() : nameKeysByRegistryId;
        Path bundleRoot = EmiPaths.resolve(outputDir, "");
        List<String> sortedItemIds = itemIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .distinct()
                .toList();
        Set<String> fluids = fluidRegistryIds == null ? Set.of() : fluidRegistryIds;
        List<String> locales = resolveLocales(languages);
        if (sortedItemIds.isEmpty() || locales.isEmpty()) {
            MweMod.LOGGER.warn(
                    "{} skipped: {} items, {} locales",
                    Log.ITEMS_LANG,
                    sortedItemIds.size(),
                    locales.size());
            return List.of();
        }

        Path searchRoot = bundleRoot.resolve(Constants.ITEMS_LANG_DIR);
        if (writes.usesFileTree()) {
            Files.createDirectories(searchRoot);
        }

        Map<String, String> enUs = langTableForLocale(langTables, Constants.DEFAULT_LANGUAGE);
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            MweMod.LOGGER.info(
                    "{} {}: building {} items ...",
                    Log.ITEMS_LANG,
                    normalized,
                    sortedItemIds.size());
            long startedAt = System.currentTimeMillis();
            Map<String, String> current = langTableForLocale(langTables, normalized);
            Map<String, String> fallback = Constants.DEFAULT_LANGUAGE.equals(normalized)
                    ? Map.of()
                    : enUs;
            RegistryResolver currentResolver = new RegistryResolver(current, fallback, nameKeys);
            RegistryResolver enResolver = isChineseLocale(normalized) && !Constants.DEFAULT_LANGUAGE.equals(normalized)
                    ? new RegistryResolver(enUs, Map.of(), nameKeys)
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

        return List.copyOf(writtenLocales);
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
            for (String py : tokensForLabel(nameCurrent)) {
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

    /** Read indexed item ids from a bundle on disk (tools / re-export only). */
    public static Set<String> readIndexedItemIds(Path bundleRoot) throws IOException {
        Path indexPath = bundleRoot.resolve(Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        ItemIndex index = JsonIO.read(indexPath, ItemIndex.class);
        return index.registryItemIds();
    }

    private static List<String> resolveLocales(List<String> languages) {
        if (languages != null && !languages.isEmpty()) {
            return languages.stream().map(ItemsLangExporter::normalizeLocale).distinct().sorted().toList();
        }
        return List.of(Constants.DEFAULT_LANGUAGE);
    }

    private static Map<String, String> langTableForLocale(
            Map<String, Map<String, String>> langTablesByLocale, String locale) {
        if (langTablesByLocale == null || langTablesByLocale.isEmpty()) {
            return Map.of();
        }
        String normalized = normalizeLocale(locale);
        Map<String, String> exact = langTablesByLocale.get(normalized);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Map<String, String>> entry : langTablesByLocale.entrySet()) {
            if (normalizeLocale(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return Map.of();
    }

    private static volatile boolean warnedUnavailable;
    private static volatile HanyuPinyinOutputFormat format;

    private static HanyuPinyinOutputFormat pinyinFormat() {
        HanyuPinyinOutputFormat cached = format;
        if (cached != null) {
            return cached;
        }
        try {
            HanyuPinyinOutputFormat created = new HanyuPinyinOutputFormat();
            created.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            created.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            format = created;
            return created;
        } catch (Throwable t) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                MweMod.LOGGER.warn(
                        "{} pinyin4j unavailable — Chinese search haystack will omit pinyin tokens: {}",
                        Log.ITEMS_LANG,
                        t.toString());
            }
            return null;
        }
    }

    private static boolean containsHan(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private static List<String> tokensForLabel(String label) {
        try {
            return tokensForLabelInner(label);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static List<String> tokensForLabelInner(String label) throws BadHanyuPinyinOutputFormatCombination {
        if (!containsHan(label)) {
            return List.of();
        }
        HanyuPinyinOutputFormat fmt = pinyinFormat();
        if (fmt == null) {
            return List.of();
        }
        List<String> syllables = new ArrayList<>();
        for (int offset = 0; offset < label.length();) {
            int cp = label.codePointAt(offset);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                String ch = new String(Character.toChars(cp));
                String[] py = PinyinHelper.toHanyuPinyinStringArray(ch.charAt(0), fmt);
                if (py != null && py.length > 0 && !py[0].isBlank()) {
                    syllables.add(py[0]);
                }
            }
            offset += Character.charCount(cp);
        }
        if (syllables.isEmpty()) {
            return List.of();
        }
        String spaced = String.join(" ", syllables);
        String continuous = String.join("", syllables);
        if (continuous.equals(spaced)) {
            return List.of(spaced);
        }
        return List.of(spaced, continuous);
    }
}
