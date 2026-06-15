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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public final class ItemsLangExporter {

    private static final int BUILD_THREADS =
            Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
    private static final int BUILD_PARALLEL_MIN_ITEMS = 512;

    private static final ExecutorService LOCALE_EXECUTOR = Executors.newFixedThreadPool(
            BUILD_THREADS,
            namedDaemonFactory("mwe-items-lang"));

    private ItemsLangExporter() {
    }

    public static CompletableFuture<List<String>> exportAsync(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds,
            Map<String, Map<String, String>> langTablesByLocale,
            Map<String, String> nameKeysByRegistryId,
            ExportWriteQueue writes) {
        Objects.requireNonNull(writes, "writes");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exportBlocking(
                        outputDir,
                        languages,
                        itemIds,
                        fluidRegistryIds,
                        langTablesByLocale,
                        nameKeysByRegistryId,
                        writes);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static List<String> export(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds,
            Map<String, Map<String, String>> langTablesByLocale,
            Map<String, String> nameKeysByRegistryId,
            ExportWriteQueue writes) throws IOException {
        try {
            return exportAsync(
                            outputDir,
                            languages,
                            itemIds,
                            fluidRegistryIds,
                            langTablesByLocale,
                            nameKeysByRegistryId,
                            writes)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("items-lang export interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("items-lang export failed: " + cause.getMessage(), cause);
        }
    }

    private static List<String> exportBlocking(
            Path outputDir,
            List<String> languages,
            Collection<String> itemIds,
            Set<String> fluidRegistryIds,
            Map<String, Map<String, String>> langTablesByLocale,
            Map<String, String> nameKeysByRegistryId,
            ExportWriteQueue writes) throws IOException {
        Map<String, Map<String, String>> langTables =
                langTablesByLocale == null ? Map.of() : langTablesByLocale;
        Map<String, String> nameKeys = nameKeysByRegistryId == null ? Map.of() : nameKeysByRegistryId;
        Path bundleRoot = EmiPaths.resolve(outputDir, "");
        List<String> sortedItemIds = itemIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .distinct()
                .toList();
        Set<String> fluids = fluidRegistryIds == null ? Set.of() : Set.copyOf(fluidRegistryIds);
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
        boolean parallelLocales = locales.size() > 1;
        boolean parallelEntries = !parallelLocales;
        long startedAt = System.currentTimeMillis();
        MweMod.LOGGER.info(
                "{} building {} items for {} locales{} ...",
                Log.ITEMS_LANG,
                sortedItemIds.size(),
                locales.size(),
                parallelLocales ? " (parallel locales)" : "");

        List<String> writtenLocales = Collections.synchronizedList(new ArrayList<>(locales.size()));
        List<Future<?>> localeTasks = new ArrayList<>(locales.size());

        for (String locale : locales) {
            localeTasks.add(LOCALE_EXECUTOR.submit(() -> {
                String normalized = normalizeLocale(locale);
                exportLocale(
                        searchRoot,
                        sortedItemIds,
                        fluids,
                        normalized,
                        langTables,
                        enUs,
                        nameKeys,
                        parallelEntries,
                        writes);
                writtenLocales.add(normalized);
            }));
        }

        RuntimeException failure = null;
        for (Future<?> task : localeTasks) {
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failure = new RuntimeException("items-lang locale build interrupted", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                failure = new RuntimeException("items-lang locale build failed: " + cause.getMessage(), cause);
            }
        }
        if (failure != null) {
            throw failure;
        }

        List<String> sortedWritten = writtenLocales.stream().sorted().toList();
        MweMod.LOGGER.info(
                "{} {} locales done ({} ms)",
                Log.ITEMS_LANG,
                sortedWritten.size(),
                System.currentTimeMillis() - startedAt);
        return sortedWritten;
    }

    private static void exportLocale(
            Path searchRoot,
            List<String> sortedItemIds,
            Set<String> fluids,
            String normalized,
            Map<String, Map<String, String>> langTables,
            Map<String, String> enUs,
            Map<String, String> nameKeys,
            boolean parallelEntries,
            ExportWriteQueue writes) {
        long startedAt = System.currentTimeMillis();
        Map<String, String> current = langTableForLocale(langTables, normalized);
        Map<String, String> fallback =
                Constants.DEFAULT_LANGUAGE.equals(normalized) ? Map.of() : enUs;
        RegistryResolver currentResolver = new RegistryResolver(current, fallback, nameKeys);
        RegistryResolver enResolver = isChineseLocale(normalized) && !Constants.DEFAULT_LANGUAGE.equals(normalized)
                ? new RegistryResolver(enUs, Map.of(), nameKeys)
                : null;

        List<ItemsLangEntry> entries = buildEntries(
                sortedItemIds,
                fluids,
                normalized,
                currentResolver,
                enResolver,
                normalized,
                startedAt,
                parallelEntries);
        MweMod.LOGGER.info(
                "{} {}: {} done ({} ms)",
                Log.ITEMS_LANG,
                normalized,
                sortedItemIds.size(),
                System.currentTimeMillis() - startedAt);

        ItemsLang payload = ItemsLang.of(normalized, entries);
        Path out = searchRoot.resolve(normalized + ".json");
        writes.submitJsonLine(out, payload);
        MweMod.LOGGER.info("{} {}: {} items -> {}", Log.ITEMS_LANG, normalized, sortedItemIds.size(), out);
    }

    private static List<ItemsLangEntry> buildEntries(
            List<String> sortedItemIds,
            Set<String> fluids,
            String normalized,
            RegistryResolver currentResolver,
            RegistryResolver enResolver,
            String localeLabel,
            long startedAt,
            boolean parallelEntries) {
        int size = sortedItemIds.size();
        ItemsLangEntry[] entries = new ItemsLangEntry[size];
        int threads = Math.min(size, BUILD_THREADS);
        if (!parallelEntries || threads <= 1 || size < BUILD_PARALLEL_MIN_ITEMS) {
            for (int i = 0; i < size; i++) {
                entries[i] = buildEntry(sortedItemIds.get(i), fluids, normalized, currentResolver, enResolver);
                logBuildProgress(i + 1, size, localeLabel, startedAt);
            }
            return Arrays.asList(entries);
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads, namedDaemonFactory("mwe-items-lang-entry"));
        AtomicInteger progress = new AtomicInteger();
        try {
            int chunkSize = Math.max(256, (size + threads - 1) / threads);
            List<Future<?>> futures = new ArrayList<>();
            for (int start = 0; start < size; start += chunkSize) {
                int from = start;
                int to = Math.min(size, start + chunkSize);
                futures.add(pool.submit(() -> {
                    for (int i = from; i < to; i++) {
                        entries[i] = buildEntry(sortedItemIds.get(i), fluids, normalized, currentResolver, enResolver);
                        int n = progress.incrementAndGet();
                        if (n % Constants.SEARCH_INDEX_PROGRESS_EVERY == 0) {
                            logBuildProgress(n, size, localeLabel, startedAt);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("items-lang build interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("items-lang build failed: " + cause.getMessage(), cause);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
        return Arrays.asList(entries);
    }

    private static ItemsLangEntry buildEntry(
            String id,
            Set<String> fluids,
            String normalized,
            RegistryResolver currentResolver,
            RegistryResolver enResolver) {
        String kind = resolveRegistryKind(id, fluids, currentResolver);
        String label = currentResolver.translateRegistry(id, kind);
        return new ItemsLangEntry(id, label, buildHaystack(id, kind, normalized, currentResolver, enResolver));
    }

    private static void logBuildProgress(int progress, int total, String localeLabel, long startedAt) {
        MweMod.LOGGER.info(
                "{} {}: {}/{} ({} ms)",
                Log.ITEMS_LANG,
                localeLabel,
                progress,
                total,
                System.currentTimeMillis() - startedAt);
    }

    private static ThreadFactory namedDaemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
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
