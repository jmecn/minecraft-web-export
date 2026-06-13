package io.github.jmecn.minecraftwebexport.emi.item;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.item.SearchIndexWriter;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import io.github.jmecn.minecraftwebexport.emi.recipe.IndexIds;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class IndexWriter {
    private static final com.google.gson.Gson GSON = io.github.jmecn.minecraftwebexport.emi.bundle.Gson.GSON;
    private static final String SCAN_LOG_STRIDE_PROPERTY = "minecraftWebExport.itemsIndexScanLogStride";
    private static final String WRITE_LOG_STRIDE_PROPERTY = "minecraftWebExport.itemsIndexWriteLogStride";

    private IndexWriter() {
    }

    public record Result(int itemCount, int inputsIndexed, int outputsIndexed, long indexBytes) {
    }

    public static Result export(Path outputDir) throws IOException {
        return export(outputDir, null, BundleMods.empty());
    }

    public static Result export(Path outputDir, MinecraftServer server) throws IOException {
        return export(outputDir, server, BundleMods.empty());
    }

    public static Result export(Path outputDir, MinecraftServer server, BundleMods mods) throws IOException {
        if (mods == null || mods.isEmpty()) {
            MinecraftWebExportMod.LOGGER.warn("{} no recipe mods in bundle - skipping items index", Log.EMI_ITEMS);
            return new Result(0, 0, 0, 0);
        }
        List<String> recipeIds = IndexIds.allRecipeIds(outputDir, mods);
        return exportWithLayouts(outputDir, server, recipeIds, recipeId -> {
            try {
                return IndexIds.loadLayout(outputDir, recipeId, mods);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Set.of());
    }

    public static Result export(
            Path outputDir,
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId) throws IOException {
        return export(outputDir, server, layoutsByRecipeId, Set.of());
    }

    public static Set<String> collectReferencedItemIds(
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds,
            Set<String> seedTagIds) {
        Set<String> itemIds = new TreeSet<>();
        mergeSeedItemIds(itemIds, seedItemIds);
        if (server != null && seedTagIds != null && !seedTagIds.isEmpty()) {
            itemIds.addAll(ClosureExpander.expand(server, seedTagIds).items());
        }
        if (layoutsByRecipeId == null || layoutsByRecipeId.isEmpty()) {
            return Set.copyOf(itemIds);
        }

        List<String> recipeIds = new ArrayList<>(layoutsByRecipeId.keySet());
        Collections.sort(recipeIds);
        Set<String> fluidRegistryIds = new TreeSet<>();

        for (String recipeId : recipeIds) {
            if (isEmiTagDisplayRecipe(recipeId)) {
                continue;
            }
            JsonObject layout = layoutsByRecipeId.get(recipeId);
            if (layout == null) {
                continue;
            }
            JsonArray widgets = readWidgets(layout);
            for (JsonElement widgetElement : widgets) {
                if (!widgetElement.isJsonObject()) {
                    continue;
                }
                JsonObject widget = widgetElement.getAsJsonObject();
                Set<String> ids = new TreeSet<>();
                if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                    addCanonicalId(widget.get("tagDisplayItem").getAsString(), ids);
                }
                if (widget.has("ingredient")) {
                    collectIngredientIds(widget.get("ingredient"), ids, Map.of(), fluidRegistryIds, server);
                }
                itemIds.addAll(ids);
            }
        }
        return Set.copyOf(itemIds);
    }

    public static Set<String> collectRegistryTagIds(MinecraftServer server, Set<String> itemIds) {
        if (server == null || itemIds == null || itemIds.isEmpty()) {
            return Set.of();
        }
        Set<String> tags = new TreeSet<>();
        for (RegistryTagSets tagSets : resolveRegistryTags(itemIds, server).values()) {
            tags.addAll(tagSets.items());
            tags.addAll(tagSets.blocks());
            tags.addAll(tagSets.fluids());
        }
        return Set.copyOf(tags);
    }

    public static Set<String> planFullModeTagExport(
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds,
            Set<String> layoutReferencedTags) {
        if (server == null) {
            return layoutReferencedTags == null ? Set.of() : Set.copyOf(layoutReferencedTags);
        }
        Set<String> items = new TreeSet<>(collectReferencedItemIds(
                server, layoutsByRecipeId, seedItemIds, layoutReferencedTags));
        Set<String> tags = new TreeSet<>(layoutReferencedTags == null ? Set.of() : layoutReferencedTags);

        while (true) {
            Set<String> nextTags = new TreeSet<>(tags);
            nextTags.addAll(collectRegistryTagIds(server, items));

            Set<String> nextItems = new TreeSet<>(items);
            if (!nextTags.isEmpty()) {
                nextItems.addAll(ClosureExpander.expand(server, nextTags).items());
            }

            if (nextTags.equals(tags) && nextItems.equals(items)) {
                return Set.copyOf(nextTags);
            }
            tags = nextTags;
            items = nextItems;
        }
    }

    public static Result export(
            Path outputDir,
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds) throws IOException {
        if (layoutsByRecipeId == null || layoutsByRecipeId.isEmpty()) {
            MinecraftWebExportMod.LOGGER.warn("{} no in-memory layouts - skipping items index", Log.EMI_ITEMS);
            return new Result(0, 0, 0, 0);
        }
        List<String> recipeIds = new ArrayList<>(layoutsByRecipeId.keySet());
        Collections.sort(recipeIds);
        return exportWithLayouts(outputDir, server, recipeIds, layoutsByRecipeId::get, seedItemIds);
    }

    @FunctionalInterface
    private interface LayoutLoader {
        JsonObject load(String recipeId);
    }

    private static Result exportWithLayouts(
            Path outputDir,
            MinecraftServer server,
            List<String> recipeIds,
            LayoutLoader layoutLookup,
            Set<String> seedItemIds) throws IOException {
        Path itemsIndexFile = Paths.resolve(outputDir, Paths.ITEMS_INDEX_FILE);
        Map<String, Set<String>> tagItems = loadTagItems(outputDir);
        ExportedTagSets exportedTagSets = loadExportedTagSets(outputDir);

        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluidRegistryIds = new TreeSet<>();
        int scanTotal = recipeIds.size();
        int scanStride = ProgressLog.stride(scanTotal, SCAN_LOG_STRIDE_PROPERTY, 20, 200);
        int scanProgress = 0;
        MinecraftWebExportMod.LOGGER.info("{} scanning {} recipes for item refs", Log.EMI_ITEMS, scanTotal);

        for (String recipeId : recipeIds) {
            scanProgress++;
            if (isEmiTagDisplayRecipe(recipeId)) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
            }
            JsonObject layout = layoutLookup.load(recipeId);
            if (layout == null) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
            }
            String categoryId = readCategoryId(layout, recipeId);
            JsonArray widgets = readWidgets(layout);

            for (JsonElement widgetElement : widgets) {
                if (!widgetElement.isJsonObject()) {
                    continue;
                }
                JsonObject widget = widgetElement.getAsJsonObject();
                Map<String, Map<String, Set<String>>> bucket = bucketForRole(widget, inputs, outputs);
                if (bucket == null) {
                    continue;
                }

                Set<String> ids = new TreeSet<>();
                if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                    addCanonicalId(widget.get("tagDisplayItem").getAsString(), ids);
                }
                if (widget.has("ingredient")) {
                    collectIngredientIds(widget.get("ingredient"), ids, tagItems, fluidRegistryIds, null);
                }
                addRecipeRefs(bucket, categoryId, ids, recipeId);
            }
            logScanProgress(scanProgress, scanTotal, scanStride);
        }

        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(inputs.keySet());
        allItemIds.addAll(outputs.keySet());
        mergeSeedItemIds(allItemIds, seedItemIds);
        MinecraftWebExportMod.LOGGER.info("{} resolving registry tags for {} items", Log.EMI_ITEMS, allItemIds.size());
        Map<String, RegistryTagSets> registryTagSetsByItem = resolveRegistryTags(allItemIds, server);

        int inputRefs = 0;
        int outputRefs = 0;
        Map<String, Set<String>> indexBuckets = new TreeMap<>();
        int writeTotal = allItemIds.size();
        int writeStride = ProgressLog.stride(writeTotal, WRITE_LOG_STRIDE_PROPERTY, 20, 200);
        int writeProgress = 0;
        int skippedHiddenItems = 0;
        MinecraftWebExportMod.LOGGER.info("{} writing {} item detail files", Log.EMI_ITEMS, writeTotal);

        for (String itemId : allItemIds) {
            writeProgress++;
            if (!Visibility.shouldExportRegistryId(server, itemId)) {
                skippedHiddenItems++;
                continue;
            }
            IdParts item = IdParts.parse(itemId);
            if (item == null) {
                continue;
            }
            indexBuckets.computeIfAbsent(item.namespace(), ignored -> new TreeSet<>()).add(item.path());
            Path itemFile = item.toItemFile(outputDir);
            Files.createDirectories(itemFile.getParent());

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("schema", 2);
            if (inputs.containsKey(itemId)) {
                Map<String, Object> byCategory = categoryBucketsToJson(inputs.get(itemId));
                detail.put("inputs", byCategory);
                inputRefs += countRecipeRefs(inputs.get(itemId));
            }
            if (outputs.containsKey(itemId)) {
                Map<String, Object> byCategory = categoryBucketsToJson(outputs.get(itemId));
                detail.put("outputs", byCategory);
                outputRefs += countRecipeRefs(outputs.get(itemId));
            }

            RegistryTagSets tagSets = registryTagSetsByItem.get(itemId);
            if (tagSets != null && tagSets.hasAny()) {
                detail.put("tags", tagSets.asJsonMap());
                detail.put("tagsInBundle", tagSets.intersection(exportedTagSets).asJsonMap());
            }

            Files.writeString(itemFile, GSON.toJson(detail));
            if (ProgressLog.shouldLog(writeProgress, writeTotal, writeStride)) {
                int pct = ProgressLog.percent(writeProgress, writeTotal);
                MinecraftWebExportMod.LOGGER.info(
                        "{} write {}% {}/{} item files",
                        Log.EMI_ITEMS,
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
        if (!fluidRegistryIds.isEmpty()) {
            root.put(SearchIndexWriter.FLUID_REGISTRY_IDS_KEY, new ArrayList<>(fluidRegistryIds));
        }

        String json = GSON.toJson(root);
        Files.createDirectories(itemsIndexFile.getParent());
        Files.writeString(itemsIndexFile, json);
        if (skippedHiddenItems > 0) {
            MinecraftWebExportMod.LOGGER.info(
                    "{} item visibility: {} indexed, {} skipped (hidden_from_recipe_viewers)",
                    Log.EMI_ITEMS,
                    indexBuckets.values().stream().mapToInt(Set::size).sum(),
                    skippedHiddenItems);
        }
        MinecraftWebExportMod.LOGGER.info(
                "{} {} items ({} input refs, {} output refs) -> {}",
                Log.EMI_ITEMS,
                allItemIds.size() - skippedHiddenItems,
                inputRefs,
                outputRefs,
                itemsIndexFile);
        int indexedCount = indexBuckets.values().stream().mapToInt(Set::size).sum();
        return new Result(indexedCount, inputRefs, outputRefs, json.length());
    }

    private static void mergeSeedItemIds(Set<String> allItemIds, Set<String> seedItemIds) {
        if (seedItemIds == null || seedItemIds.isEmpty()) {
            return;
        }
        int before = allItemIds.size();
        for (String raw : seedItemIds) {
            addCanonicalId(raw, allItemIds);
        }
        int added = allItemIds.size() - before;
        if (added > 0) {
            MinecraftWebExportMod.LOGGER.info("{} merged {} seed/closure items ({} new)", Log.EMI_ITEMS, seedItemIds.size(), added);
        }
    }

    private static void logScanProgress(int progress, int total, int stride) {
        if (ProgressLog.shouldLog(progress, total, stride)) {
            int pct = ProgressLog.percent(progress, total);
            MinecraftWebExportMod.LOGGER.info(
                    "{} scan {}% {}/{} recipes",
                    Log.EMI_ITEMS,
                    pct,
                    progress,
                    total);
        }
    }

    private static Map<String, Set<String>> loadTagItems(Path outputDir) throws IOException {
        Path tagsRoot = Paths.resolve(outputDir, Paths.TAGS_DIR);
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
        Path tagsRoot = Paths.resolve(outputDir, Paths.TAGS_DIR);
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
            return Paths.resolve(outputDir, "items/" + namespace + "/" + path + ".json");
        }
    }

    private static void collectIngredientIds(
            JsonElement ingredient,
            Set<String> out,
            Map<String, Set<String>> tagItems,
            Set<String> fluidRegistryIds) {
        collectIngredientIds(ingredient, out, tagItems, fluidRegistryIds, null);
    }

    private static void collectIngredientIds(
            JsonElement ingredient,
            Set<String> out,
            Map<String, Set<String>> tagItems,
            Set<String> fluidRegistryIds,
            MinecraftServer server) {
        if (ingredient == null || ingredient.isJsonNull()) {
            return;
        }
        if (ingredient.isJsonPrimitive() && ingredient.getAsJsonPrimitive().isString()) {
            String raw = ingredient.getAsString().trim();
            if (raw.startsWith("item:")) {
                addCanonicalId(raw.substring(5), out);
            } else if (raw.startsWith("fluid:")) {
                addFluidRegistryId(raw.substring(6), out, fluidRegistryIds);
            } else if (raw.startsWith("#item:")) {
                expandTagIngredient(raw.substring(6), out, tagItems, server);
            } else if (raw.contains(":") && !raw.startsWith("#")) {
                addCanonicalId(raw, out);
            }
            return;
        }
        if (ingredient.isJsonArray()) {
            for (JsonElement child : ingredient.getAsJsonArray()) {
                collectIngredientIds(child, out, tagItems, fluidRegistryIds, server);
            }
            return;
        }
        if (!ingredient.isJsonObject()) {
            return;
        }

        JsonObject obj = ingredient.getAsJsonObject();
        if (obj.has("type") && obj.get("type").isJsonPrimitive() && obj.has("id") && obj.get("id").isJsonPrimitive()) {
            String kind = obj.get("type").getAsString();
            if ("item".equals(kind)) {
                addCanonicalId(obj.get("id").getAsString(), out);
            } else if ("fluid".equals(kind)) {
                addFluidRegistryId(obj.get("id").getAsString(), out, fluidRegistryIds);
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
                    expandTagIngredient(entry.get("tag").getAsString(), out, tagItems, server);
                }
                if (entry.has("fluid") && entry.get("fluid").isJsonObject()) {
                    JsonObject fluid = entry.getAsJsonObject("fluid");
                    if (fluid.has("id") && fluid.get("id").isJsonPrimitive()) {
                        addFluidRegistryId(fluid.get("id").getAsString(), out, fluidRegistryIds);
                    }
                }
            }
        }
    }

    private static void expandTagIngredient(
            String tagId,
            Set<String> out,
            Map<String, Set<String>> tagItems,
            MinecraftServer server) {
        if (server != null) {
            out.addAll(ClosureExpander.expand(server, Set.of(tagId)).items());
            return;
        }
        out.addAll(tagItems.getOrDefault(tagId, Set.of()));
    }

    private static void addFluidRegistryId(String raw, Set<String> out, Set<String> fluidRegistryIds) {
        String id = canonicalRegistryId(raw);
        if (id == null || id.isBlank()) {
            return;
        }
        out.add(id);
        if (fluidRegistryIds != null) {
            fluidRegistryIds.add(id);
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

    private static String readCategoryId(JsonObject layout, String recipeId) {
        if (layout != null && layout.has("category") && layout.get("category").isJsonPrimitive()) {
            String category = layout.get("category").getAsString().trim();
            if (!category.isEmpty()) {
                return category;
            }
        }
        int slash = recipeId.indexOf('/');
        if (slash > 0) {
            return recipeId.substring(0, slash);
        }
        return "emi:unknown";
    }

    private static Map<String, Object> categoryBucketsToJson(Map<String, Set<String>> byCategory) {
        Map<String, Object> json = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : byCategory.entrySet()) {
            json.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return json;
    }

    private static int countRecipeRefs(Map<String, Set<String>> byCategory) {
        int total = 0;
        for (Set<String> refs : byCategory.values()) {
            total += refs.size();
        }
        return total;
    }

    private static Map<String, Map<String, Set<String>>> bucketForRole(
            JsonObject widget,
            Map<String, Map<String, Set<String>>> inputs,
            Map<String, Map<String, Set<String>>> outputs) {
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

    private static void addRecipeRefs(
            Map<String, Map<String, Set<String>>> bucket,
            String categoryId,
            Set<String> ids,
            String recipeId) {
        for (String id : ids) {
            bucket.computeIfAbsent(id, ignored -> new TreeMap<>())
                    .computeIfAbsent(categoryId, ignored -> new TreeSet<>())
                    .add(recipeId);
        }
    }

    static boolean isEmiTagDisplayRecipe(String recipeId) {
        return recipeId != null && recipeId.startsWith("emi:/tag/");
    }
}
