package io.github.jmecn.minecraftwebexport.emi.tag;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembers;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembersResult;
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

public final class MembersIndexWriter {

    private MembersIndexWriter() {
    }


    public static boolean isEnabled() {
        return !MweConfig.skipTagMembersIndexExport();
    }

    public static TagMembersResult export(
            Path outputDir, MinecraftServer server, Set<String> tagIds, ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        Path tagsDir = EmiPaths.resolve(outputDir, Constants.TAGS_DIR);
        Files.createDirectories(tagsDir);

        int memberRefs = 0;
        long totalBytes = 0;
        int itemTagEntries = 0;
        int blockTagEntries = 0;
        int fluidTagEntries = 0;
        Set<String> itemTags = new TreeSet<>();
        Set<String> blockTags = new TreeSet<>();
        Set<String> fluidTags = new TreeSet<>();

        for (String tagRef : new TreeSet<>(tagIds)) {
            String tagId = ClosureExpander.normalizeTagRef(tagRef);
            ParsedTagId parsed = ParsedTagId.parse(tagId);
            if (parsed == null) continue;

            TagMembers members = ClosureExpander.expandTagMembers(server, tagRef);

            WriteOutcome itemOutcome = writeTagFile(outputDir, parsed, TagKind.ITEMS, members.items(), writes);
            if (itemOutcome.written()) {
                itemTagEntries++;
                itemTags.add(parsed.tagId());
                memberRefs += itemOutcome.memberRefs();
                totalBytes += itemOutcome.bytes();
            }

            WriteOutcome blockOutcome = writeTagFile(outputDir, parsed, TagKind.BLOCKS, members.blocks(), writes);
            if (blockOutcome.written()) {
                blockTagEntries++;
                blockTags.add(parsed.tagId());
                memberRefs += blockOutcome.memberRefs();
                totalBytes += blockOutcome.bytes();
            }

            WriteOutcome fluidOutcome = writeTagFile(
                    outputDir, parsed, TagKind.FLUIDS, filterRedundantFlowingFluids(members.fluids()), writes);
            if (fluidOutcome.written()) {
                fluidTagEntries++;
                fluidTags.add(parsed.tagId());
                memberRefs += fluidOutcome.memberRefs();
                totalBytes += fluidOutcome.bytes();
            }
        }

        long catalogBytes = writeTagsCatalog(outputDir, itemTags, blockTags, fluidTags, writes);
        totalBytes += catalogBytes;

        MweMod.LOGGER.info(
                "{} tags: {} refs -> {} item, {} block, {} fluid files (catalog {} ids, {} member refs, {} bytes)",
                Log.INDEX_TAGS,
                tagIds.size(),
                itemTagEntries,
                blockTagEntries,
                fluidTagEntries,
                itemTags.size() + blockTags.size() + fluidTags.size(),
                memberRefs,
                totalBytes);

        return new TagMembersResult(
                tagIds.size(),
                itemTagEntries,
                blockTagEntries,
                fluidTagEntries,
                memberRefs,
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
            Set<String> fluidTags) throws IOException {
        try (ExportWriteQueue writes = new ExportWriteQueue()) {
            long bytes = writeTagsCatalog(outputDir, itemTags, blockTags, fluidTags, writes);
            writes.awaitIdle();
            return bytes;
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

    private static WriteOutcome writeTagFile(
            Path outputDir,
            ParsedTagId parsed,
            TagKind kind,
            Set<String> values,
            ExportWriteQueue writes) {
        if (values == null || values.isEmpty()) {
            return WriteOutcome.EMPTY;
        }
        TagValues document = new TagValues(new ArrayList<>(values));

        Path outFile = parsed.toTagFilePath(outputDir, kind);
        writes.submitJson(outFile, document);
        return new WriteOutcome(true, values.size(), JsonIO.toUtf8Bytes(document).length);
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

    private record WriteOutcome(boolean written, int memberRefs, int bytes) {
        static final WriteOutcome EMPTY = new WriteOutcome(false, 0, 0);
    }

    private enum TagKind {
        ITEMS("items"),
        BLOCKS("blocks"),
        FLUIDS("fluids");

        private final String dirName;

        TagKind(String dirName) {
            this.dirName = dirName;
        }
    }

    private record ParsedTagId(String namespace, String path) {

        static ParsedTagId parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String[] parts = raw.trim().split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            if (parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            return new ParsedTagId(parts[0], parts[1]);
        }

        String tagId() {
            return namespace + ":" + path;
        }

        Path toTagFilePath(Path outputDir, TagKind kind) {
            return EmiPaths.resolve(
                    outputDir,
                    Constants.TAGS_DIR + "/" + namespace + "/" + kind.dirName + "/" + path + ".json");
        }
    }
}
