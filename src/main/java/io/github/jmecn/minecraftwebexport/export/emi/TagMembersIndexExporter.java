package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class TagMembersIndexExporter {

    private static final Logger LOGGER = LogManager.getLogger(TagMembersIndexExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private TagMembersIndexExporter() {
    }

    public record Result(
            int tagsIndexed,
            int itemTagEntries,
            int blockTagEntries,
            int fluidTagEntries,
            int totalMemberRefs,
            long tagFileBytes,
            long catalogIndexBytes,
            Set<String> itemTags,
            Set<String> blockTags,
            Set<String> fluidTags) {
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipTagMembersIndexExport");
    }

    public static Result export(Path outputDir, MinecraftServer server, Set<String> tagIds) throws IOException {
        Path tagsDir = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAGS_DIR);
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
            String tagId = TagClosureExpander.normalizeTagRef(tagRef);
            ParsedTagId parsed = ParsedTagId.parse(tagId);
            if (parsed == null) continue;

            TagClosureExpander.TagMembers members = TagClosureExpander.expandTagMembers(server, tagRef);

            WriteOutcome itemOutcome = writeTagFile(outputDir, parsed, TagKind.ITEMS, members.items());
            if (itemOutcome.written()) {
                itemTagEntries++;
                itemTags.add(parsed.tagId());
                memberRefs += itemOutcome.memberRefs();
                totalBytes += itemOutcome.bytes();
            }

            WriteOutcome blockOutcome = writeTagFile(outputDir, parsed, TagKind.BLOCKS, members.blocks());
            if (blockOutcome.written()) {
                blockTagEntries++;
                blockTags.add(parsed.tagId());
                memberRefs += blockOutcome.memberRefs();
                totalBytes += blockOutcome.bytes();
            }

            WriteOutcome fluidOutcome = writeTagFile(outputDir, parsed, TagKind.FLUIDS, members.fluids());
            if (fluidOutcome.written()) {
                fluidTagEntries++;
                fluidTags.add(parsed.tagId());
                memberRefs += fluidOutcome.memberRefs();
                totalBytes += fluidOutcome.bytes();
            }
        }

        long catalogBytes = writeTagsCatalog(outputDir, itemTags, blockTags, fluidTags);
        totalBytes += catalogBytes;

        LOGGER.info(
                "{} tags: {} refs -> {} item, {} block, {} fluid files (catalog {} ids, {} member refs, {} bytes)",
                ExportLog.INDEX_TAGS,
                tagIds.size(),
                itemTagEntries,
                blockTagEntries,
                fluidTagEntries,
                itemTags.size() + blockTags.size() + fluidTags.size(),
                memberRefs,
                totalBytes);

        return new Result(
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

    /**
     * §5.7.2 catalog for tag list/search; popover still uses per-tag files only.
     */
    static long writeTagsCatalog(
            Path outputDir,
            Set<String> itemTags,
            Set<String> blockTags,
            Set<String> fluidTags) throws IOException {
        if (itemTags.isEmpty() && blockTags.isEmpty() && fluidTags.isEmpty()) {
            return 0;
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        if (!itemTags.isEmpty()) {
            root.put("items", new ArrayList<>(itemTags));
        }
        if (!blockTags.isEmpty()) {
            root.put("blocks", new ArrayList<>(blockTags));
        }
        if (!fluidTags.isEmpty()) {
            root.put("fluids", new ArrayList<>(fluidTags));
        }
        Path indexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAGS_INDEX_FILE);
        Files.createDirectories(indexFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(indexFile, json);
        return json.length();
    }

    private static WriteOutcome writeTagFile(Path outputDir, ParsedTagId parsed, TagKind kind, Set<String> values)
            throws IOException {
        if (values == null || values.isEmpty()) {
            return WriteOutcome.EMPTY;
        }
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        root.add("values", array);

        Path outFile = parsed.toTagFilePath(outputDir, kind);
        Files.createDirectories(outFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(outFile, json);
        return new WriteOutcome(true, values.size(), json.length());
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
            return EmiBundlePaths.resolve(
                    outputDir,
                    EmiBundlePaths.TAGS_DIR + "/" + namespace + "/" + kind.dirName + "/" + path + ".json");
        }
    }
}
