package io.github.jmecn.minecraftwebexport.emi.item;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembers;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import io.github.jmecn.minecraftwebexport.emi.recipe.IndexIds;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsItemsIndexFromLayoutWidgets() throws IOException {
        BundleMods mods = IndexIds.writeFixtureLayout(
                tempDir,
                "minecraft:iron_pickaxe",
                layout("""
                        {
                          "schema": 2,
                          "id": "minecraft:iron_pickaxe",
                          "category": "minecraft:crafting",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "input",
                              "ingredient": "item:minecraft:stick"
                            },
                            {
                              "type": "slot",
                              "role": "input",
                              "ingredient": {
                                "type": "item",
                                "id": "minecraft:iron_ingot"
                              }
                            },
                            {
                              "type": "slot",
                              "role": "output",
                              "ingredient": "item:minecraft:iron_pickaxe"
                            }
                          ]
                        }
                        """));

        ItemIndexResult result = IndexWriter.export(tempDir, null, mods);

        assertEquals(3, result.itemCount());
        assertEquals(2, result.inputsIndexed());
        assertEquals(1, result.outputsIndexed());

        Path itemsIndexFile = Paths.resolve(tempDir, Constants.ITEMS_INDEX_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(itemsIndexFile)).getAsJsonObject();
        Path stickFile = Paths.resolve(tempDir, "items/minecraft/stick.json");
        Path pickaxeFile = Paths.resolve(tempDir, "items/minecraft/iron_pickaxe.json");
        JsonObject stick = JsonParser.parseString(Files.readString(stickFile)).getAsJsonObject();
        JsonObject pickaxe = JsonParser.parseString(Files.readString(pickaxeFile)).getAsJsonObject();

        assertEquals(1, json.get("schema").getAsInt());
        assertEquals(3, json.getAsJsonArray("minecraft").size());
        assertEquals(
                "minecraft:iron_pickaxe",
                stick.getAsJsonObject("inputs")
                        .getAsJsonArray("minecraft:crafting")
                        .get(0)
                        .getAsString());
        assertEquals(
                "minecraft:iron_pickaxe",
                pickaxe.getAsJsonObject("outputs")
                        .getAsJsonArray("minecraft:crafting")
                        .get(0)
                        .getAsString());
    }

    @Test
    void expandsTagInputsUsingTagMembersIndex() throws IOException {
        BundleMods mods = IndexIds.writeFixtureLayout(
                tempDir,
                "tfc:glassworking_jar",
                layout("""
                        {
                          "schema": 2,
                          "id": "tfc:glassworking_jar",
                          "category": "tfc:glassworking",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "input",
                              "ingredient": "#item:tfc:glass_batches_tier_2",
                              "tagDisplayItem": "tfc:silica_glass_batch"
                            }
                          ]
                        }
                        """));

        Path tagFile = Paths.resolve(tempDir, "tags/tfc/items/glass_batches_tier_2.json");
        Files.createDirectories(tagFile.getParent());
        Files.writeString(tagFile, """
                {
                  "values": [
                    "tfc:silica_glass_batch",
                    "tfc:hematitic_glass_batch"
                  ]
                }
                """);

        IndexWriter.export(tempDir, null, mods);

        Path silicaFile = Paths.resolve(tempDir, "items/tfc/silica_glass_batch.json");
        Path hematiticFile = Paths.resolve(tempDir, "items/tfc/hematitic_glass_batch.json");
        JsonObject hematitic = JsonParser.parseString(Files.readString(hematiticFile)).getAsJsonObject();

        assertTrue(Files.exists(silicaFile));
        assertTrue(Files.exists(hematiticFile));
        assertEquals(
                "tfc:glassworking_jar",
                hematitic.getAsJsonObject("inputs")
                        .getAsJsonArray("tfc:glassworking")
                        .get(0)
                        .getAsString());
    }

    @Test
    void skipsEmiTagDisplayRecipesInReverseIndex() throws IOException {
        BundleMods mods = IndexIds.writeFixtureLayout(
                tempDir,
                "emi:/tag/item/minecraft/rails",
                layout("""
                        {
                          "schema": 2,
                          "id": "emi:/tag/item/minecraft/rails",
                          "category": "emi:tag",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "input",
                              "tagDisplayItem": "minecraft:activator_rail"
                            }
                          ]
                        }
                        """));
        mods = mergeMods(mods, IndexIds.writeFixtureLayout(
                tempDir,
                "minecraft:activator_rail",
                layout("""
                        {
                          "schema": 2,
                          "id": "minecraft:activator_rail",
                          "category": "minecraft:crafting",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "output",
                              "ingredient": "item:minecraft:activator_rail"
                            }
                          ]
                        }
                        """)));

        IndexWriter.export(tempDir, null, mods);

        Path railFile = Paths.resolve(tempDir, "items/minecraft/activator_rail.json");
        JsonObject rail = JsonParser.parseString(Files.readString(railFile)).getAsJsonObject();
        assertTrue(rail.has("outputs"));
        assertEquals(
                1,
                rail.getAsJsonObject("outputs").getAsJsonArray("minecraft:crafting").size());
        assertEquals(
                "minecraft:activator_rail",
                rail.getAsJsonObject("outputs").getAsJsonArray("minecraft:crafting").get(0).getAsString());
        assertTrue(!rail.has("inputs") || rail.getAsJsonObject("inputs").isEmpty());
    }

    @Test
    void collectReferencedItemIdsIncludesLayoutAndSeedItems() {
        Map<String, JsonObject> layouts = Map.of(
                "minecraft:stick",
                layout("""
                        {
                          "schema": 2,
                          "id": "minecraft:stick",
                          "category": "minecraft:crafting",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "output",
                              "ingredient": "item:minecraft:stick"
                            }
                          ]
                        }
                        """));

        Set<String> itemIds = IndexWriter.collectReferencedItemIds(
                null,
                layouts,
                Set.of("minecraft:oak_log"),
                Set.of());

        assertEquals(Set.of("minecraft:oak_log", "minecraft:stick"), itemIds);
    }

    @Test
    void collectRegistryTagIdsReturnsEmptyWithoutServer() {
        assertTrue(IndexWriter.collectRegistryTagIds(null, Set.of("minecraft:stick")).isEmpty());
    }

    @Test
    void mergesSeedItemIdsWithoutRecipeRefs() throws IOException {
        BundleMods mods = BundleMods.empty();
        IndexWriter.export(
                tempDir,
                null,
                Map.of(
                        "minecraft:stick",
                        layout("""
                                {
                                  "schema": 2,
                                  "id": "minecraft:stick",
                                  "category": "minecraft:crafting",
                                  "widgets": [
                                    {
                                      "type": "slot",
                                      "role": "output",
                                      "ingredient": "item:minecraft:stick"
                                    }
                                  ]
                                }
                                """)),
                Set.of("minecraft:oak_log"));

        Path oakFile = Paths.resolve(tempDir, "items/minecraft/oak_log.json");
        Path itemsIndexFile = Paths.resolve(tempDir, Constants.ITEMS_INDEX_FILE);
        JsonObject index = JsonParser.parseString(Files.readString(itemsIndexFile)).getAsJsonObject();

        assertTrue(Files.exists(oakFile));
        assertTrue(index.getAsJsonArray("minecraft").contains(JsonParser.parseString("\"oak_log\"")));
        JsonObject oak = JsonParser.parseString(Files.readString(oakFile)).getAsJsonObject();
        assertFalse(oak.has("inputs"));
        assertFalse(oak.has("outputs"));
    }

    private static JsonObject layout(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static BundleMods mergeMods(BundleMods first, BundleMods second) {
        BundleMods.Builder builder = BundleMods.builder();
        first.mods().forEach(builder::put);
        second.mods().forEach(builder::put);
        return builder.build();
    }
}
