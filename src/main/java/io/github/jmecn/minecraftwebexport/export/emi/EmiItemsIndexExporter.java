package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public final class EmiItemsIndexExporter {

    private static final Logger LOGGER = Logger.getLogger(EmiItemsIndexExporter.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EmiItemsIndexExporter() {
    }

    public record Result(int itemCount, int inputsIndexed, int outputsIndexed, long indexBytes) {
    }

    public static Result export(Path outputDir) throws IOException {
        Path recipeIndexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.RECIPE_INDEX_FILE);
        Path itemsIndexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.ITEMS_INDEX_FILE);

        if (!Files.isRegularFile(recipeIndexFile)) {
            LOGGER.warning("[emi-items] missing " + recipeIndexFile + " - skipping items index");
            return new Result(0, 0, 0, 0);
        }

        JsonObject recipeIndex = JsonParser.parseString(Files.readString(recipeIndexFile)).getAsJsonObject();
        JsonObject recipeEntries = recipeIndex.has("recipes") && recipeIndex.get("recipes").isJsonObject()
                ? recipeIndex.getAsJsonObject("recipes")
                : new JsonObject();
        Map<String, Set<String>> tagItems = loadTagItems(outputDir);

        Map<String, Set<String>> inputs = new TreeMap<>();
        Map<String, Set<String>> outputs = new TreeMap<>();

        for (var entry : recipeEntries.entrySet()) {
            String recipeId = entry.getKey();
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject meta = entry.getValue().getAsJsonObject();
            Path layoutPath = resolveLayoutPath(outputDir, recipeId, meta);
            if (!Files.isRegularFile(layoutPath)) {
                continue;
            }
            JsonObject layout = JsonParser.parseString(Files.readString(layoutPath)).getAsJsonObject();
            JsonArray widgets = layout.has("widgets") && layout.get("widgets").isJsonArray()
                    ? layout.getAsJsonArray("widgets")
                    : new JsonArray();

            for (JsonElement widgetElement : widgets) {
                if (!widgetElement.isJsonObject()) {
                    continue;
                }
                JsonObject widget = widgetElement.getAsJsonObject();
                String role = widget.has("role") ? widget.get("role").getAsString() : "";
                Map<String, Set<String>> bucket = "output".equals(role)
                        ? outputs
                        : ("input".equals(role) || "catalyst".equals(role) ? inputs : null);
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
                for (String id : ids) {
                    bucket.computeIfAbsent(id, ignored -> new TreeSet<>()).add(recipeId);
                }
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("itemCount", 0);
        Map<String, Object> items = new TreeMap<>();
        root.put("items", items);

        int inputRefs = 0;
        int outputRefs = 0;
        Set<String> allIds = new TreeSet<>();
        allIds.addAll(inputs.keySet());
        allIds.addAll(outputs.keySet());
        for (String itemId : allIds) {
            Set<String> inputRecipes = inputs.getOrDefault(itemId, Set.of());
            Set<String> outputRecipes = outputs.getOrDefault(itemId, Set.of());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("inputs", inputRecipes.stream().sorted().toList());
            item.put("outputs", outputRecipes.stream().sorted().toList());
            items.put(itemId, item);
            inputRefs += inputRecipes.size();
            outputRefs += outputRecipes.size();
        }
        root.put("itemCount", items.size());

        Files.createDirectories(itemsIndexFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(itemsIndexFile, json);
        LOGGER.info("[emi-items] " + items.size()
                + " items (" + inputRefs + " input refs, " + outputRefs + " output refs) -> "
                + itemsIndexFile);
        return new Result(items.size(), inputRefs, outputRefs, json.length());
    }

    private static Path resolveLayoutPath(Path outputDir, String recipeId, JsonObject meta) {
        if (meta.has("layout") && meta.get("layout").isJsonPrimitive()) {
            return EmiBundlePaths.resolve(outputDir, meta.get("layout").getAsString());
        }
        return EmiBundlePaths.resolve(
                outputDir,
                RecipeLayoutPaths.LAYOUTS_DIR + "/" + RecipeLayoutPaths.relativeLayoutJson(recipeId));
    }

    private static Map<String, Set<String>> loadTagItems(Path outputDir) throws IOException {
        Path tagMembersFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.TAG_MEMBERS_FILE);
        if (!Files.isRegularFile(tagMembersFile)) {
            return Map.of();
        }

        JsonObject root = JsonParser.parseString(Files.readString(tagMembersFile)).getAsJsonObject();
        JsonObject items = root.has("items") && root.get("items").isJsonObject()
                ? root.getAsJsonObject("items")
                : new JsonObject();
        Map<String, Set<String>> tagItems = new TreeMap<>();
        for (var entry : items.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            Set<String> members = new TreeSet<>();
            for (JsonElement member : entry.getValue().getAsJsonArray()) {
                if (member.isJsonPrimitive()) {
                    addCanonicalId(member.getAsString(), members);
                }
            }
            tagItems.put(entry.getKey(), members);
        }
        return tagItems;
    }

    private static void collectIngredientIds(JsonElement ingredient, Set<String> out, Map<String, Set<String>> tagItems) {
        if (ingredient == null || ingredient.isJsonNull()) {
            return;
        }
        if (ingredient.isJsonPrimitive() && ingredient.getAsJsonPrimitive().isString()) {
            String raw = ingredient.getAsString().trim();
            if (raw.startsWith("item:")) {
                addCanonicalId(raw.substring(5), out);
            } else if (raw.startsWith("#item:")) {
                out.addAll(tagItems.getOrDefault(raw.substring(6), Set.of()));
            } else if (!raw.startsWith("#item:") && raw.contains(":") && !raw.startsWith("#")) {
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
}
