package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander;
import io.github.jmecn.minecraftwebexport.model.emi.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
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

final class ItemIndexSupport {

    private ItemIndexSupport() {
    }

    static void mergeSeedItemIds(Set<String> allItemIds, Set<String> seedItemIds) {
        if (seedItemIds == null || seedItemIds.isEmpty()) {
            return;
        }
        for (String raw : seedItemIds) {
            addCanonicalId(raw, allItemIds);
        }
    }

    static Map<String, Set<String>> loadTagItems(Path outputDir) throws IOException {
        Path tagsRoot = EmiPaths.resolve(outputDir, Constants.TAGS_DIR);
        if (!Files.isDirectory(tagsRoot)) {
            return new HashMap<>();
        }

        Map<String, Set<String>> tagItems = new TreeMap<>();
        try (Stream<Path> files = Files.walk(tagsRoot)) {
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

    static ExportedTagCatalog loadExportedTagCatalog(Path outputDir) throws IOException {
        Path tagsRoot = EmiPaths.resolve(outputDir, Constants.TAGS_DIR);
        if (!Files.isDirectory(tagsRoot)) {
            return ExportedTagCatalog.empty();
        }
        Set<String> items = new TreeSet<>();
        Set<String> blocks = new TreeSet<>();
        Set<String> fluids = new TreeSet<>();
        try (Stream<Path> files = Files.walk(tagsRoot)) {
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
        return new ExportedTagCatalog(items, blocks, fluids);
    }

    static Map<String, RegistryTagSet> resolveRegistryTags(Set<String> itemIds, MinecraftServer server) {
        if (server == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Registry<Item> itemRegistry = server.registryAccess().registryOrThrow(Registries.ITEM);
        Registry<Block> blockRegistry = server.registryAccess().registryOrThrow(Registries.BLOCK);
        Registry<Fluid> fluidRegistry = server.registryAccess().registryOrThrow(Registries.FLUID);
        Map<String, RegistryTagSet> out = new TreeMap<>();

        for (String itemId : itemIds) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation == null) {
                continue;
            }
            Optional<ResourceKey<Item>> itemKey = itemRegistry.getResourceKey(itemRegistry.get(resourceLocation));
            if (itemKey.isEmpty()) {
                continue;
            }
            Optional<? extends Holder<Item>> holder = itemRegistry.getHolder(itemKey.get());
            if (holder.isEmpty()) {
                continue;
            }
            Set<String> itemTags = new TreeSet<>();
            Set<String> blockTags = new TreeSet<>();
            Set<String> fluidTags = new TreeSet<>();
            holder.get().tags().forEach(tag -> itemTags.add(tag.location().toString()));

            Item item = holder.get().value();
            if (item instanceof BlockItem blockItem) {
                blockRegistry.getResourceKey(blockItem.getBlock())
                        .flatMap(blockRegistry::getHolder)
                        .ifPresent(blockHolder -> blockHolder.tags()
                                .forEach(tag -> blockTags.add(tag.location().toString())));
            }
            if (item instanceof BucketItem bucketItem) {
                Fluid fluid = bucketItem.getFluid();
                fluidRegistry.getResourceKey(fluid)
                        .flatMap(fluidRegistry::getHolder)
                        .ifPresent(fluidHolder -> fluidHolder.tags()
                                .forEach(tag -> fluidTags.add(tag.location().toString())));
            }
            out.put(itemId, RegistryTagSet.of(itemTags, blockTags, fluidTags));
        }
        return out;
    }

    static Set<String> collectRegistryTagIds(MinecraftServer server, Set<String> itemIds) {
        if (server == null || itemIds == null || itemIds.isEmpty()) {
            return Set.of();
        }
        Set<String> tags = new TreeSet<>();
        for (RegistryTagSet tagSet : resolveRegistryTags(itemIds, server).values()) {
            if (tagSet.items() != null) {
                tags.addAll(tagSet.items());
            }
            if (tagSet.blocks() != null) {
                tags.addAll(tagSet.blocks());
            }
            if (tagSet.fluids() != null) {
                tags.addAll(tagSet.fluids());
            }
        }
        return Set.copyOf(tags);
    }

    static void collectIngredientIds(
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

    static void expandTagIngredient(
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

    static void addFluidRegistryId(String raw, Set<String> out, Set<String> fluidRegistryIds) {
        String id = canonicalRegistryId(raw);
        if (id == null || id.isBlank()) {
            return;
        }
        out.add(id);
        if (fluidRegistryIds != null) {
            fluidRegistryIds.add(id);
        }
    }

    static void addCanonicalId(String raw, Set<String> out) {
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

    static JsonArray readWidgets(JsonObject layout) {
        JsonElement widgetsElement = layout.get("widgets");
        if (widgetsElement != null && widgetsElement.isJsonArray()) {
            return widgetsElement.getAsJsonArray();
        }
        return new JsonArray();
    }

    static String readCategoryId(JsonObject layout, String recipeId) {
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

    static Map<String, List<String>> categoryBuckets(Map<String, Set<String>> byCategory) {
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : byCategory.entrySet()) {
            buckets.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return buckets;
    }

    static int countRecipeRefs(Map<String, Set<String>> byCategory) {
        int total = 0;
        for (Set<String> refs : byCategory.values()) {
            total += refs.size();
        }
        return total;
    }

    static Map<String, Map<String, Set<String>>> bucketForRole(
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

    static void addRecipeRefs(
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

    enum TagKind {
        ITEMS("items"),
        BLOCKS("blocks"),
        FLUIDS("fluids");

        private final String dirName;

        TagKind(String dirName) {
            this.dirName = dirName;
        }
    }

    record TagFileRef(TagKind kind, String namespace, String path) {
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

    record IdParts(String namespace, String path) {
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
            return EmiPaths.resolve(outputDir, "items/" + namespace + "/" + path + ".json");
        }
    }
}
