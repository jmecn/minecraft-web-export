package io.github.jmecn.minecraftwebexport.emi.tag;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.tag.TagContents;
import io.github.jmecn.minecraftwebexport.model.tag.TagIndexResult;
import io.github.jmecn.minecraftwebexport.model.tag.TagWriteOutcome;
import io.github.jmecn.minecraftwebexport.model.tag.ParsedTagId;
import io.github.jmecn.minecraftwebexport.enums.tag.TagKind;
import io.github.jmecn.minecraftwebexport.model.tag.TagValues;
import io.github.jmecn.minecraftwebexport.model.tag.TagsCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

public final class TagIndexWriter {

    private TagIndexWriter() {
    }

    public static TagIndexResult export(
            Path outputDir, MinecraftServer server, Set<String> tagIds, ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
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

            TagWriteOutcome itemOutcome = writeTagFile(outputDir, parsed, TagKind.ITEMS, contents.items(), writes);
            if (itemOutcome.written()) {
                itemTagEntries++;
                itemTags.add(parsed.tagId());
                registryRefs += itemOutcome.registryRefs();
                totalBytes += itemOutcome.bytes();
            }

            TagWriteOutcome blockOutcome = writeTagFile(outputDir, parsed, TagKind.BLOCKS, contents.blocks(), writes);
            if (blockOutcome.written()) {
                blockTagEntries++;
                blockTags.add(parsed.tagId());
                registryRefs += blockOutcome.registryRefs();
                totalBytes += blockOutcome.bytes();
            }

            TagWriteOutcome fluidOutcome = writeTagFile(
                    outputDir, parsed, TagKind.FLUIDS, filterRedundantFlowingFluids(contents.fluids()), writes);
            if (fluidOutcome.written()) {
                fluidTagEntries++;
                fluidTags.add(parsed.tagId());
                registryRefs += fluidOutcome.registryRefs();
                totalBytes += fluidOutcome.bytes();
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

        return new TagIndexResult(
                tagIds.size(),
                itemTagEntries,
                blockTagEntries,
                fluidTagEntries,
                registryRefs,
                totalBytes - catalogBytes,
                catalogBytes,
                Set.copyOf(itemTags),
                Set.copyOf(blockTags),
                Set.copyOf(fluidTags));
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

    private static TagWriteOutcome writeTagFile(
            Path outputDir,
            ParsedTagId parsed,
            TagKind kind,
            Set<String> values,
            ExportWriteQueue writes) {
        if (values == null || values.isEmpty()) {
            return TagWriteOutcome.EMPTY;
        }
        TagValues document = new TagValues(new ArrayList<>(values));

        Path outFile = parsed.toTagFilePath(outputDir, kind);
        writes.submitJson(outFile, document);
        return new TagWriteOutcome(true, values.size(), JsonIO.toUtf8Bytes(document).length);
    }

    /**
     * Minecraft fluid tags list both still and flowing variants; the web viewer only exports still fluids.
     */
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
