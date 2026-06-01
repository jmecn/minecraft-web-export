package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects lang keys referenced by the bundle (recipe meta, item/tag/category indexes).
 * Used for FULL export lang merge — aligned with {@code emi-bundle-optimize} lang prune.
 */
public final class LangUsedKeysCollector {

    private static final Pattern FLUID_NBT_NAME = Pattern.compile("FluidName:\"([^\"]+)\"");

    private final Set<String> keys = new TreeSet<>();

    public int size() {
        return keys.size();
    }

    public Set<String> snapshot() {
        return Set.copyOf(keys);
    }

    public void collectMeta(JsonObject meta) {
        if (meta == null) {
            return;
        }
        JsonElement widgets = meta.get("widgets");
        if (widgets == null || !widgets.isJsonArray()) {
            return;
        }
        for (JsonElement element : widgets.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject widget = element.getAsJsonObject();
            if (widget.has("interaction") && widget.get("interaction").isJsonObject()) {
                collectInteraction(widget.getAsJsonObject("interaction"));
            }
        }
    }

    public void collectFromCategoriesIndex(Path outputDir) throws IOException {
        Path indexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.CATEGORIES_INDEX_FILE);
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        JsonObject manifest = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        JsonElement categories = manifest.get("categories");
        if (categories == null || !categories.isJsonArray()) {
            return;
        }
        for (JsonElement element : categories.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            if (entry.has("nameKey") && entry.get("nameKey").isJsonPrimitive()) {
                keys.add(entry.get("nameKey").getAsString());
            }
            if (entry.has("iconItem") && entry.get("iconItem").isJsonPrimitive()) {
                addRegistryItem(entry.get("iconItem").getAsString());
            }
            if (entry.has("iconKey") && entry.get("iconKey").isJsonPrimitive()) {
                addRegistryItem(entry.get("iconKey").getAsString());
            }
        }
    }

    public void collectFromItemsIndex(Path outputDir) throws IOException {
        Path indexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        for (var entry : index.entrySet()) {
            String ns = entry.getKey();
            if ("schema".equals(ns)) {
                continue;
            }
            if ("fluid".equals(ns) && entry.getValue().isJsonArray()) {
                for (JsonElement fluidEl : entry.getValue().getAsJsonArray()) {
                    if (fluidEl.isJsonPrimitive()) {
                        String fluidPath = fluidEl.getAsString();
                        if (!fluidPath.isEmpty()) {
                            addRegistryFluid(fluidPath.contains(":") ? fluidPath : "minecraft:" + fluidPath);
                        }
                    }
                }
                continue;
            }
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement pathEl : entry.getValue().getAsJsonArray()) {
                if (pathEl.isJsonPrimitive()) {
                    String path = pathEl.getAsString();
                    if (!path.isEmpty()) {
                        addRegistryItem(path.contains(":") ? path : ns + ":" + path);
                    }
                }
            }
        }
    }

    public void collectFromTagsIndex(Path outputDir) throws IOException {
        Path indexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAGS_INDEX_FILE);
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        JsonObject tagsIndex = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        for (String bucket : new String[] { "items", "blocks", "fluids" }) {
            JsonElement array = tagsIndex.get(bucket);
            if (array == null || !array.isJsonArray()) {
                continue;
            }
            for (JsonElement tagEl : array.getAsJsonArray()) {
                if (tagEl.isJsonPrimitive()) {
                    addTag(tagEl.getAsString());
                }
            }
        }
    }

    private void collectCategory(JsonObject meta) {
        if (meta.has("category") && meta.get("category").isJsonPrimitive()) {
            keys.add(CategoryLangKeys.guessNameKey(meta.get("category").getAsString()));
        }
    }

    private void collectInteraction(JsonObject interaction) {
        if (interaction == null) {
            return;
        }
        String kind = stringProp(interaction, "kind");
        if ("item".equals(kind)) {
            if (interaction.has("id") && interaction.get("id").isJsonPrimitive()) {
                addRegistryItem(interaction.get("id").getAsString());
            }
            if (interaction.has("nbt")) {
                collectFluidFromNbt(interaction.get("nbt"));
            }
            return;
        }
        if ("fluid".equals(kind)) {
            if (interaction.has("id") && interaction.get("id").isJsonPrimitive()) {
                addRegistryFluid(interaction.get("id").getAsString());
            }
            return;
        }
        if ("tag".equals(kind)) {
            if (interaction.has("tag") && interaction.get("tag").isJsonPrimitive()) {
                addTag(interaction.get("tag").getAsString());
            }
            if (interaction.has("displayId") && interaction.get("displayId").isJsonPrimitive()) {
                addRegistryItem(interaction.get("displayId").getAsString());
            }
            return;
        }
        if ("list".equals(kind) && interaction.has("entries") && interaction.get("entries").isJsonArray()) {
            for (JsonElement entry : interaction.get("entries").getAsJsonArray()) {
                if (entry.isJsonObject()) {
                    collectInteraction(entry.getAsJsonObject());
                }
            }
        }
    }

    private void collectFluidFromNbt(JsonElement nbt) {
        String raw = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        Matcher matcher = FLUID_NBT_NAME.matcher(raw);
        if (matcher.find()) {
            addRegistryFluid(matcher.group(1));
        }
    }

    private void addRegistryItem(String registryId) {
        LangClosureKeys.addForItem(keys, registryId);
    }

    private void addRegistryFluid(String registryId) {
        LangClosureKeys.addForFluid(keys, registryId);
    }

    private void addTag(String tagId) {
        if (tagId == null || tagId.isEmpty()) {
            return;
        }
        String dotted = tagId.replace('/', '.').replace(':', '.');
        keys.add("tag.item." + dotted);
        keys.add("tag.block." + dotted);
        keys.add("tag.fluid." + dotted);
    }

    private static String stringProp(JsonObject object, String key) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return null;
    }
}
