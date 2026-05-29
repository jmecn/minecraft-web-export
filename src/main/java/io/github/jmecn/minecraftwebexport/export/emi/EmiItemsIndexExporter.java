package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class EmiItemsIndexExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiItemsIndexExporter.class);
    private static final Gson GSON = ExportGson.GSON;
    private static final String SCAN_LOG_STRIDE_PROPERTY = "minecraftWebExport.itemsIndexScanLogStride";
    private static final String WRITE_LOG_STRIDE_PROPERTY = "minecraftWebExport.itemsIndexWriteLogStride";

    private EmiItemsIndexExporter() {
    }

    public record Result(int itemCount, int inputsIndexed, int outputsIndexed, long indexBytes) {
    }

    public static Result export(Path outputDir) throws IOException {
        return export(outputDir, null, RecipeBundleMods.empty());
    }

    public static Result export(Path outputDir, MinecraftServer server) throws IOException {
        return export(outputDir, server, RecipeBundleMods.empty());
    }

    public static Result export(Path outputDir, MinecraftServer server, RecipeBundleMods mods) throws IOException {
        if (mods == null || mods.isEmpty()) {
            LOGGER.warn("{} no recipe mods in bundle - skipping items index", ExportLog.EMI_ITEMS);
            return new Result(0, 0, 0, 0);
        }

        Path itemsIndexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.ITEMS_INDEX_FILE);
        List<String> recipeIds = RecipeIndexIds.allRecipeIds(outputDir, mods);
        Map<String, Set<String>> tagItems = loadTagItems(outputDir);
        ExportedTagSets exportedTagSets = loadExportedTagSets(outputDir);

        Map<String, Set<String>> inputs = new TreeMap<>();
        Map<String, Set<String>> outputs = new TreeMap<>();

        RecipeLayoutLookup layoutLookup = new RecipeLayoutLookup(outputDir, mods);
        int scanTotal = recipeIds.size();
        int scanStride = ExportProgressLog.stride(scanTotal, SCAN_LOG_STRIDE_PROPERTY, 20, 200);
        int scanProgress = 0;
        LOGGER.info("{} scanning {} recipes for item refs", ExportLog.EMI_ITEMS, scanTotal);

        for (String recipeId : recipeIds) {
            scanProgress++;
            if (isEmiTagDisplayRecipe(recipeId)) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
            }
            JsonObject layout = layoutLookup.loadLayout(recipeId);
            if (layout == null) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
            }
            JsonArray widgets = readWidgets(layout);

            for (JsonElement widgetElement : widgets) {
                if (!widgetElement.isJsonObject()) {
                    continue;
                }
                JsonObject widget = widgetElement.getAsJsonObject();
                Map<String, Set<String>> bucket = bucketForRole(widget, inputs, outputs);
                if (bucket == null) {
                    continue;
                }

                Set<String> ids = new TreeSet<>();
                if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                    addCanonicalId(widget.get("tagDisplayItem").getAsString(), ids);
                }
                if (widget.has("ingredient")) {
                    collectIngredientIds(widget.get("ingredient"), ids, tagItems);
                }
                addRecipeRefs(bucket, ids, recipeId);
            }
            logScanProgress(scanProgress, scanTotal, scanStride);
        }

        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(inputs.keySet());
        allItemIds.addAll(outputs.keySet());
        LOGGER.info("{} resolving registry tags for {} items", ExportLog.EMI_ITEMS, allItemIds.size());
        Map<String, RegistryTagSets> registryTagSetsByItem = resolveRegistryTags(allItemIds, server);

        int inputRefs = 0;
        int outputRefs = 0;
        Map<String, Set<String>> indexBuckets = new TreeMap<>();
        int writeTotal = allItemIds.size();
        int writeStride = ExportProgressLog.stride(writeTotal, WRITE_LOG_STRIDE_PROPERTY, 20, 200);
        int writeProgress = 0;
        LOGGER.info("{} writing {} item detail files", ExportLog.EMI_ITEMS, writeTotal);

        for (String itemId : allItemIds) {
            writeProgress++;
            IdParts item = IdParts.parse(itemId);
            if (item == null) {
                continue;
            }
            indexBuckets.computeIfAbsent(item.namespace(), ignored -> new TreeSet<>()).add(item.path());
            Path itemFile = item.toItemFile(outputDir);
            Files.createDirectories(itemFile.getParent());

            Map<String, Object> detail = new LinkedHashMap<>();
            if (inputs.containsKey(itemId)) {
                detail.put("inputs", new TreeSet<>(inputs.get(itemId)));
                inputRefs += inputs.get(itemId).size();
            }
            if (outputs.containsKey(itemId)) {
                detail.put("outputs", new TreeSet<>(outputs.get(itemId)));
                outputRefs += outputs.get(itemId).size();
            }

            RegistryTagSets tagSets = registryTagSetsByItem.get(itemId);
            if (tagSets != null && tagSets.hasAny()) {
                detail.put("tags", tagSets.asJsonMap());
                detail.put("tagsInBundle", tagSets.intersection(exportedTagSets).asJsonMap());
            }

            Files.writeString(itemFile, GSON.toJson(detail));
            if (ExportProgressLog.shouldLog(writeProgress, writeTotal, writeStride)) {
                int pct = ExportProgressLog.percent(writeProgress, writeTotal);
                LOGGER.info(
                        "{} write {}% {}/{} item files",
                        ExportLog.EMI_ITEMS,
                        pct,
                        writeProgress,
                        writeTotal);
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        for (Map.Entry<String, Set<String>> entry : indexBuckets.entrySet()) {
            root.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        String json = GSON.toJson(root);
        Files.createDirectories(itemsIndexFile.getParent());
        Files.writeString(itemsIndexFile, json);
        LOGGER.info(
                "{} {} items ({} input refs, {} output refs) -> {}",
                ExportLog.EMI_ITEMS,
                allItemIds.size(),
                inputRefs,
                outputRefs,
                itemsIndexFile);
        return new Result(allItemIds.size(), inputRefs, outputRefs, json.length());
    }

    private static void logScanProgress(int progress, int total, int stride) {
        if (ExportProgressLog.shouldLog(progress, total, stride)) {
            int pct = ExportProgressLog.percent(progress, total);
            LOGGER.info(
                    "{} scan {}% {}/{} recipes",
                    ExportLog.EMI_ITEMS,
                    pct,
                    progress,
                    total);
        }
    }

    private static Map<String, Set<String>> loadTagItems(Path outputDir) throws IOException {
        Path tagsRoot = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAGS_DIR);
        if (!Files.isDirectory(tagsRoot)) {
            return Map.of();
        }

        Map<String, Set<String>> tagItems = new TreeMap<>();
        try (var files = Files.walk(tagsRoot)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (!file.getFileName().toString().endsWith(".json")) {
                    continue;
                }
                TagFileRef ref = TagFileRef.fromPath(tagsRoot, file);
                if (ref == null || ref.kind() != TagKind.ITEMS) {
                    continue;
                }
                JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (!root.has("values") || !root.get("values").isJsonArray()) {
                    continue;
                }
                Set<String> members = new TreeSet<>();
                for (JsonElement element : root.getAsJsonArray("values")) {
                    if (element.isJsonPrimitive()) {
                        String id = canonicalRegistryId(element.getAsString());
                        if (id != null && !id.isBlank()) {
                            members.add(id);
                        }
                    }
                }
                tagItems.put(ref.tagId(), members);
            }
        }
        return tagItems;
    }

    private static ExportedTagSets loadExportedTagSets(Path outputDir) throws IOException {
        Path tagsRoot = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAGS_DIR);
        if (!Files.isDirectory(tagsRoot)) {
            return ExportedTagSets.empty();
        }
        Set<String> items = new TreeSet<>();
        Set<String> blocks = new TreeSet<>();
        Set<String> fluids = new TreeSet<>();
        try (var files = Files.walk(tagsRoot)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (!file.getFileName().toString().endsWith(".json")) {
                    continue;
                }
                TagFileRef ref = TagFileRef.fromPath(tagsRoot, file);
                if (ref == null) {
                    continue;
                }
                switch (ref.kind()) {
                    case ITEMS -> items.add(ref.tagId());
                    case BLOCKS -> blocks.add(ref.tagId());
                    case FLUIDS -> fluids.add(ref.tagId());
                }
            }
        }
        return new ExportedTagSets(items, blocks, fluids);
    }

    private static Map<String, RegistryTagSets> resolveRegistryTags(Set<String> itemIds, MinecraftServer server) {
        if (server == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Registry<Item> itemRegistry = server.registryAccess().registryOrThrow(Registries.ITEM);
        Registry<Block> blockRegistry = server.registryAccess().registryOrThrow(Registries.BLOCK);
        Registry<Fluid> fluidRegistry = server.registryAccess().registryOrThrow(Registries.FLUID);
        Map<String, RegistryTagSets> out = new TreeMap<>();

        for (String itemId : itemIds) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation == null) {
                continue;
            }
            Optional<ResourceKey<Item>> itemKey = itemRegistry.getResourceKey(itemRegistry.get(resourceLocation));
            if (itemKey.isEmpty()) {
                continue;
            }
            Optional<? extends net.minecraft.core.Holder<Item>> holder = itemRegistry.getHolder(itemKey.get());
            if (holder.isEmpty()) {
                continue;
            }
            RegistryTagSets tags = new RegistryTagSets();
            holder.get().tags().forEach(tag -> tags.items().add(tag.location().toString()));

            Item item = holder.get().value();
            if (item instanceof BlockItem blockItem) {
                blockRegistry.getResourceKey(blockItem.getBlock())
                        .flatMap(blockRegistry::getHolder)
                        .ifPresent(blockHolder -> blockHolder.tags()
                                .forEach(tag -> tags.blocks().add(tag.location().toString())));
            }
            if (item instanceof BucketItem bucketItem) {
                Fluid fluid = bucketItem.getFluid();
                fluidRegistry.getResourceKey(fluid)
                        .flatMap(fluidRegistry::getHolder)
                        .ifPresent(fluidHolder -> fluidHolder.tags()
                                .forEach(tag -> tags.fluids().add(tag.location().toString())));
            }
            out.put(itemId, tags);
        }
        return out;
    }

    private static final class RegistryTagSets {
        private final Set<String> items = new TreeSet<>();
        private final Set<String> blocks = new TreeSet<>();
        private final Set<String> fluids = new TreeSet<>();

        Set<String> items() {
            return items;
        }

        Set<String> blocks() {
            return blocks;
        }

        Set<String> fluids() {
            return fluids;
        }

        boolean hasAny() {
            return !items.isEmpty() || !blocks.isEmpty() || !fluids.isEmpty();
        }

        Map<String, Object> asJsonMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("items", new ArrayList<>(items));
            map.put("blocks", new ArrayList<>(blocks));
            map.put("fluids", new ArrayList<>(fluids));
            return map;
        }

        RegistryTagSets intersection(ExportedTagSets exported) {
            RegistryTagSets inBundle = new RegistryTagSets();
            for (String tag : items) {
                if (exported.items().contains(tag)) {
                    inBundle.items.add(tag);
                }
            }
            for (String tag : blocks) {
                if (exported.blocks().contains(tag)) {
                    inBundle.blocks.add(tag);
                }
            }
            for (String tag : fluids) {
                if (exported.fluids().contains(tag)) {
                    inBundle.fluids.add(tag);
                }
            }
            return inBundle;
        }
    }

    private record ExportedTagSets(Set<String> items, Set<String> blocks, Set<String> fluids) {
        static ExportedTagSets empty() {
            return new ExportedTagSets(Set.of(), Set.of(), Set.of());
        }
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

    private record TagFileRef(TagKind kind, String namespace, String path) {
        static TagFileRef fromPath(Path tagsRoot, Path file) {
            Path relative = tagsRoot.relativize(file);
            if (relative.getNameCount() < 3) {
                return null;
            }
            String namespace = relative.getName(0).toString();
            String kindName = relative.getName(1).toString();
            TagKind kind = switch (kindName) {
                case "items" -> TagKind.ITEMS;
                case "blocks" -> TagKind.BLOCKS;
                case "fluids" -> TagKind.FLUIDS;
                default -> null;
            };
            if (kind == null) {
                return null;
            }
            Path pathPart = relative.subpath(2, relative.getNameCount());
            String path = pathPart.toString().replace('\\', '/');
            if (!path.endsWith(".json")) {
                return null;
            }
            path = path.substring(0, path.length() - ".json".length());
            return new TagFileRef(kind, namespace, path);
        }

        String tagId() {
            return namespace + ":" + path;
        }
    }

    private record IdParts(String namespace, String path) {
        static IdParts parse(String fullId) {
            if (fullId == null || fullId.isBlank()) {
                return null;
            }
            int sep = fullId.indexOf(':');
            if (sep <= 0 || sep >= fullId.length() - 1) {
                return null;
            }
            return new IdParts(fullId.substring(0, sep), fullId.substring(sep + 1));
        }

        Path toItemFile(Path outputDir) {
            return EmiBundlePaths.resolve(outputDir, "items/" + namespace + "/" + path + ".json");
        }
    }

    private static void collectIngredientIds(
            JsonElement ingredient,
            Set<String> out,
            Map<String, Set<String>> tagItems) {
        if (ingredient == null || ingredient.isJsonNull()) {
            return;
        }
        if (ingredient.isJsonPrimitive() && ingredient.getAsJsonPrimitive().isString()) {
            String raw = ingredient.getAsString().trim();
            if (raw.startsWith("item:")) {
                addCanonicalId(raw.substring(5), out);
            } else if (raw.startsWith("#item:")) {
                out.addAll(tagItems.getOrDefault(raw.substring(6), Set.of()));
            } else if (raw.contains(":") && !raw.startsWith("#")) {
                addCanonicalId(raw, out);
            }
            return;
        }
        if (ingredient.isJsonArray()) {
            for (JsonElement child : ingredient.getAsJsonArray()) {
                collectIngredientIds(child, out, tagItems);
            }
            return;
        }
        if (!ingredient.isJsonObject()) {
            return;
        }

        JsonObject obj = ingredient.getAsJsonObject();
        if (obj.has("type") && obj.get("type").isJsonPrimitive() && obj.has("id") && obj.get("id").isJsonPrimitive()) {
            String kind = obj.get("type").getAsString();
            if ("item".equals(kind) || "fluid".equals(kind)) {
                addCanonicalId(obj.get("id").getAsString(), out);
            }
        }
        if (obj.has("entries") && obj.get("entries").isJsonArray()) {
            for (JsonElement entryElement : obj.getAsJsonArray("entries")) {
                if (!entryElement.isJsonObject()) {
                    continue;
                }
                JsonObject entry = entryElement.getAsJsonObject();
                if (entry.has("ids") && entry.get("ids").isJsonArray()) {
                    for (JsonElement idElement : entry.getAsJsonArray("ids")) {
                        if (idElement.isJsonPrimitive()) {
                            addCanonicalId(idElement.getAsString(), out);
                        }
                    }
                }
                if (entry.has("tag") && entry.get("tag").isJsonPrimitive()) {
                    out.addAll(tagItems.getOrDefault(entry.get("tag").getAsString(), Set.of()));
                }
                if (entry.has("fluid") && entry.get("fluid").isJsonObject()) {
                    JsonObject fluid = entry.getAsJsonObject("fluid");
                    if (fluid.has("id") && fluid.get("id").isJsonPrimitive()) {
                        addCanonicalId(fluid.get("id").getAsString(), out);
                    }
                }
            }
        }
    }

    private static void addCanonicalId(String raw, Set<String> out) {
        String id = canonicalRegistryId(raw);
        if (id != null && !id.isBlank()) {
            out.add(id);
        }
    }

    static String canonicalRegistryId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw;
        if (value.startsWith("item:")) {
            value = value.substring(5);
        }
        int brace = value.indexOf('{');
        if (brace >= 0) {
            value = value.substring(0, brace);
        }
        int at = value.indexOf('@');
        if (at >= 0) {
            value = value.substring(0, at);
        }
        return value;
    }

    private static JsonArray readWidgets(JsonObject layout) {
        JsonElement widgetsElement = layout.get("widgets");
        if (widgetsElement != null && widgetsElement.isJsonArray()) {
            return widgetsElement.getAsJsonArray();
        }
        return new JsonArray();
    }

    private static Map<String, Set<String>> bucketForRole(
            JsonObject widget,
            Map<String, Set<String>> inputs,
            Map<String, Set<String>> outputs) {
        JsonElement roleElement = widget.get("role");
        if (roleElement == null || !roleElement.isJsonPrimitive()) {
            return null;
        }
        String role = roleElement.getAsString();
        if ("output".equals(role)) {
            return outputs;
        }
        if ("input".equals(role) || "catalyst".equals(role)) {
            return inputs;
        }
        return null;
    }

    private static void addRecipeRefs(Map<String, Set<String>> bucket, Set<String> ids, String recipeId) {
        for (String id : ids) {
            bucket.computeIfAbsent(id, ignored -> new TreeSet<>()).add(recipeId);
        }
    }

    /** EMI tag pages (`emi:/tag/...`) are not crafting recipes; item reverse index lists real recipes only. */
    static boolean isEmiTagDisplayRecipe(String recipeId) {
        return recipeId != null && recipeId.startsWith("emi:/tag/");
    }
}
