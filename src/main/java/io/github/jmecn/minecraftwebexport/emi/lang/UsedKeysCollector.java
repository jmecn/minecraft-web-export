package io.github.jmecn.minecraftwebexport.emi.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysExporter;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

public final class UsedKeysCollector {

    private final Set<String> keys = new TreeSet<>();

    public int size() {
        return keys.size();
    }

    public Set<String> snapshot() {
        return Set.copyOf(keys);
    }

    public void collectMeta(RecipeMeta meta) {
        if (meta == null || meta.widgets() == null) {
            return;
        }
        for (RecipeWidget widget : meta.widgets()) {
            collectInteraction(widget.interaction());
        }
    }

    public void collectFromCategoriesIndex(Path outputDir) throws IOException {
        Path indexFile = EmiPaths.resolve(outputDir, Constants.CATEGORIES_INDEX_FILE);
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

    public void collectFromItemNameKeys(Path outputDir) throws IOException {
        for (String key : NameKeysExporter.readNameKeys(outputDir).values()) {
            if (key != null && !key.isBlank()) {
                keys.add(key);
            }
        }
    }

    public void collectFromItemsIndex(Path outputDir) throws IOException {
        Path indexFile = EmiPaths.resolve(outputDir, Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
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
        Path indexFile = EmiPaths.resolve(outputDir, Constants.TAGS_INDEX_FILE);
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

    private void collectInteraction(WidgetInteraction interaction) {
        if (interaction == null) {
            return;
        }
        String kind = interaction.kind();
        if ("item".equals(kind)) {
            if (interaction.id() != null) {
                addRegistryItem(interaction.id());
            }
            if (interaction.nbt() != null) {
                collectFluidFromNbt(interaction.nbt());
            }
            return;
        }
        if ("fluid".equals(kind)) {
            if (interaction.id() != null) {
                addRegistryFluid(interaction.id());
            }
            return;
        }
        if ("tag".equals(kind)) {
            if (interaction.tag() != null) {
                addTag(interaction.tag());
            }
            if (interaction.displayId() != null) {
                addRegistryItem(interaction.displayId());
            }
            return;
        }
        if ("list".equals(kind) && interaction.entries() != null) {
            for (WidgetInteraction entry : interaction.entries()) {
                collectInteraction(entry);
            }
        }
    }

    private void collectFluidFromNbt(JsonElement nbt) {
        String raw = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        Matcher matcher = Constants.FLUID_NBT_NAME_PATTERN.matcher(raw);
        if (matcher.find()) {
            addRegistryFluid(matcher.group(1));
        }
    }

    private void addRegistryItem(String registryId) {
        ClosureKeys.addForItem(keys, registryId);
    }

    private void addRegistryFluid(String registryId) {
        ClosureKeys.addForFluid(keys, registryId);
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
}
