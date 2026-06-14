package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.tag.TagExpander;
import io.github.jmecn.minecraftwebexport.model.tag.TagContents;
import io.github.jmecn.minecraftwebexport.model.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.item.ItemIdParts;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import io.github.jmecn.minecraftwebexport.model.tag.TagFileRef;
import io.github.jmecn.minecraftwebexport.enums.tag.TagKind;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
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

    /**
     * Scan tag files already on disk (tools / re-export only).
     * Export pipeline passes {@link ExportedTagCatalog} in memory from {@link io.github.jmecn.minecraftwebexport.emi.tag.TagIndexWriter}.
     */
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

    static void addFluidRegistryId(String raw, Set<String> out, Set<String> fluidRegistryIds) {
        String id = canonicalRegistryId(raw);
        if (!isExportableRegistryId(id)) {
            return;
        }
        out.add(id);
        if (fluidRegistryIds != null) {
            fluidRegistryIds.add(id);
        }
    }

    static void addCanonicalId(String raw, Set<String> out) {
        String id = canonicalRegistryId(raw);
        if (isExportableRegistryId(id)) {
            out.add(id);
        }
    }

    static boolean isExportableRegistryId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        if (id.startsWith("#")) {
            return false;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon >= id.length() - 1) {
            return false;
        }
        String namespace = id.substring(0, colon);
        return !namespace.startsWith("#");
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

    static String categoryFromMeta(RecipeMeta meta, String recipeId) {
        if (meta != null && meta.category() != null && !meta.category().isBlank()) {
            return meta.category();
        }
        return categoryFromRecipeId(recipeId);
    }

    static String categoryFromRecipeId(String recipeId) {
        int slash = recipeId.indexOf('/');
        if (slash > 0) {
            return recipeId.substring(0, slash);
        }
        return "emi:unknown";
    }

    static void collectInteractionIds(
            WidgetInteraction interaction,
            Set<String> out,
            Set<String> fluidRegistryIds) {
        collectInteractionIds(interaction, out, fluidRegistryIds, null, null);
    }

    static void collectInteractionIds(
            WidgetInteraction interaction,
            Set<String> out,
            Set<String> fluidRegistryIds,
            MinecraftServer server,
            Map<String, Set<String>> exportedTagMembers) {
        if (interaction == null) {
            return;
        }
        String kind = interaction.kind();
        if ("item".equals(kind)) {
            addCanonicalId(interaction.id(), out);
            return;
        }
        if ("fluid".equals(kind)) {
            addFluidRegistryId(interaction.id(), out, fluidRegistryIds);
            return;
        }
        if ("tag".equals(kind)) {
            if (interaction.displayId() != null && isExportableRegistryId(interaction.displayId())) {
                addCanonicalId(interaction.displayId(), out);
            }
            addTagMembers(interaction.tag(), interaction.tagKind(), out, fluidRegistryIds, server, exportedTagMembers);
            return;
        }
        if ("list".equals(kind) && interaction.entries() != null) {
            for (WidgetInteraction entry : interaction.entries()) {
                collectInteractionIds(entry, out, fluidRegistryIds, server, exportedTagMembers);
            }
        }
    }

    private static void addTagMembers(
            String tagId,
            String tagKind,
            Set<String> out,
            Set<String> fluidRegistryIds,
            MinecraftServer server,
            Map<String, Set<String>> exportedTagMembers) {
        if (tagId == null || tagId.isBlank()) {
            return;
        }
        Set<String> members = resolveTagMembers(tagId, tagKind, server, exportedTagMembers);
        for (String member : members) {
            if ("fluid".equals(tagKind)) {
                addFluidRegistryId(member, out, fluidRegistryIds);
            } else {
                addCanonicalId(member, out);
            }
        }
    }

    private static Set<String> resolveTagMembers(
            String tagId,
            String tagKind,
            MinecraftServer server,
            Map<String, Set<String>> exportedTagMembers) {
        if (exportedTagMembers != null) {
            Set<String> fromBundle = exportedTagMembers.get(tagId);
            if (fromBundle != null && !fromBundle.isEmpty()) {
                return fromBundle;
            }
        }
        if (server == null) {
            return Set.of();
        }
        String prefix = switch (tagKind == null ? "item" : tagKind) {
            case "fluid" -> "#fluid:";
            case "block" -> "#block:";
            default -> "#item:";
        };
        TagContents contents = TagExpander.expandTagContents(server, prefix + tagId);
        return switch (tagKind == null ? "item" : tagKind) {
            case "fluid" -> contents.fluids();
            case "block" -> contents.blocks();
            default -> contents.items();
        };
    }

    static Map<String, Map<String, Set<String>>> bucketForRole(
            String role,
            Map<String, Map<String, Set<String>>> inputs,
            Map<String, Map<String, Set<String>>> outputs) {
        if (role == null) {
            return null;
        }
        if ("output".equals(role)) {
            return outputs;
        }
        if ("input".equals(role) || "catalyst".equals(role)) {
            return inputs;
        }
        return null;
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
}
