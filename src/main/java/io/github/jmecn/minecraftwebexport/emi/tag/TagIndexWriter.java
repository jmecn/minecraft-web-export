package io.github.jmecn.minecraftwebexport.emi.tag;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.tag.TagContents;
import io.github.jmecn.minecraftwebexport.model.tag.ParsedTagId;
import io.github.jmecn.minecraftwebexport.enums.tag.TagKind;
import io.github.jmecn.minecraftwebexport.model.tag.TagValues;
import io.github.jmecn.minecraftwebexport.model.tag.TagsCatalog;
import io.github.jmecn.minecraftwebexport.model.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.pipeline.TagExportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;

public final class TagIndexWriter {

    private TagIndexWriter() {
    }

    public static TagExportResult export(
            Path outputDir, MinecraftServer server, Set<String> tagIds, ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        if (tagIds == null || tagIds.isEmpty()) {
            return TagExportResult.empty();
        }
        Path tagsDir = EmiPaths.resolve(outputDir, Constants.TAGS_DIR);
        Files.createDirectories(tagsDir);

        int registryRefs = 0;
        long totalBytes = 0;
        int itemTagEntries = 0;
        int blockTagEntries = 0;
        int fluidTagEntries = 0;
        Set<String> itemTags = new TreeSet<>();
        Set<String> blockTags = new TreeSet<>();
        Set<String> fluidTags = new TreeSet<>();

        for (String tagRef : new TreeSet<>(tagIds)) {
            String tagId = TagExpander.normalizeTagRef(tagRef);
            ParsedTagId parsed = ParsedTagId.parse(tagId);
            if (parsed == null) continue;

            TagContents contents = TagExpander.expandTagContents(server, tagRef);

            TagFileWrite itemWrite = writeTagFile(outputDir, server, parsed, TagKind.ITEMS, contents.items(), writes);
            if (itemWrite.written()) {
                itemTagEntries++;
                itemTags.add(parsed.tagId());
                registryRefs += itemWrite.registryRefs();
                totalBytes += itemWrite.bytes();
            }

            TagFileWrite blockWrite = writeTagFile(outputDir, server, parsed, TagKind.BLOCKS, contents.blocks(), writes);
            if (blockWrite.written()) {
                blockTagEntries++;
                blockTags.add(parsed.tagId());
                registryRefs += blockWrite.registryRefs();
                totalBytes += blockWrite.bytes();
            }

            TagFileWrite fluidWrite = writeTagFile(
                    outputDir,
                    server,
                    parsed,
                    TagKind.FLUIDS,
                    filterRedundantFlowingFluids(contents.fluids()),
                    writes);
            if (fluidWrite.written()) {
                fluidTagEntries++;
                fluidTags.add(parsed.tagId());
                registryRefs += fluidWrite.registryRefs();
                totalBytes += fluidWrite.bytes();
            }
        }

        long catalogBytes = writeTagsCatalog(outputDir, itemTags, blockTags, fluidTags, writes);
        totalBytes += catalogBytes;

        MweMod.LOGGER.info(
                "{} tags: {} refs -> {} item, {} block, {} fluid files (catalog {} ids, {} registry refs, {} bytes)",
                Log.INDEX_TAGS,
                tagIds.size(),
                itemTagEntries,
                blockTagEntries,
                fluidTagEntries,
                itemTags.size() + blockTags.size() + fluidTags.size(),
                registryRefs,
                totalBytes);

        return new TagExportResult(
                tagIds.size(),
                new ExportedTagCatalog(Set.copyOf(itemTags), Set.copyOf(blockTags), Set.copyOf(fluidTags)));
    }

    private record TagFileWrite(int registryRefs, long bytes) {
        private static final TagFileWrite SKIPPED = new TagFileWrite(0, 0);

        static TagFileWrite skipped() {
            return SKIPPED;
        }

        boolean written() {
            return registryRefs > 0;
        }
    }

    static long writeTagsCatalog(
            Path outputDir,
            Set<String> itemTags,
            Set<String> blockTags,
            Set<String> fluidTags,
            ExportWriteQueue writes) {
        if (itemTags.isEmpty() && blockTags.isEmpty() && fluidTags.isEmpty()) {
            return 0;
        }
        TagsCatalog catalog = TagsCatalog.of(
                new ArrayList<>(itemTags),
                new ArrayList<>(blockTags),
                new ArrayList<>(fluidTags));
        Path indexFile = EmiPaths.resolve(outputDir, Constants.TAGS_INDEX_FILE);
        writes.submitJson(indexFile, catalog);
        return JsonIO.toUtf8Bytes(catalog).length;
    }

    static Set<String> filterExportableTagMembers(MinecraftServer server, Set<String> values) {
        return filterTagMembers(values, id -> Visibility.shouldExportRegistryId(server, id));
    }

    static Set<String> filterTagMembers(Set<String> values, Predicate<String> exportable) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (String id : values) {
            if (exportable.test(id)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private static TagFileWrite writeTagFile(
            Path outputDir,
            MinecraftServer server,
            ParsedTagId parsed,
            TagKind kind,
            Set<String> values,
            ExportWriteQueue writes) {
        Set<String> exportable = filterExportableTagMembers(server, values);
        if (exportable.isEmpty()) {
            return TagFileWrite.skipped();
        }
        TagValues document = new TagValues(new ArrayList<>(exportable));

        Path outFile = parsed.toTagFilePath(outputDir, kind);
        writes.submitJson(outFile, document);
        return new TagFileWrite(exportable.size(), JsonIO.toUtf8Bytes(document).length);
    }

    static Set<String> filterRedundantFlowingFluids(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> stillIds = new HashSet<>();
        for (String id : values) {
            if (!isFlowingFluidId(id)) {
                stillIds.add(id);
            }
        }
        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (String id : values) {
            if (isFlowingFluidId(id)) {
                String stillId = stillFluidId(id);
                if (stillId != null && stillIds.contains(stillId)) {
                    continue;
                }
            }
            filtered.add(id);
        }
        return filtered;
    }

    private static boolean isFlowingFluidId(String registryId) {
        if (registryId == null || registryId.isBlank()) {
            return false;
        }
        int sep = registryId.indexOf(':');
        String path = sep >= 0 ? registryId.substring(sep + 1) : registryId;
        return path.startsWith("flowing_");
    }

    private static String stillFluidId(String flowingId) {
        int sep = flowingId.indexOf(':');
        if (sep <= 0 || sep >= flowingId.length() - 1) {
            return null;
        }
        String path = flowingId.substring(sep + 1);
        if (!path.startsWith("flowing_")) {
            return null;
        }
        return flowingId.substring(0, sep) + ":" + path.substring("flowing_".length());
    }
}
