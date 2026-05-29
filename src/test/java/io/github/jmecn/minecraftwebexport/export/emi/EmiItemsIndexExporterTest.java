package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiItemsIndexExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsItemsIndexFromLayoutWidgets() throws IOException {
        RecipeBundleMods mods = RecipeIndexIds.writeFixtureLayout(
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

        EmiItemsIndexExporter.Result result = EmiItemsIndexExporter.export(tempDir, null, mods);

        assertEquals(3, result.itemCount());
        assertEquals(2, result.inputsIndexed());
        assertEquals(1, result.outputsIndexed());

        Path itemsIndexFile = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.ITEMS_INDEX_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(itemsIndexFile)).getAsJsonObject();
        Path stickFile = EmiBundlePaths.resolve(tempDir, "items/minecraft/stick.json");
        Path pickaxeFile = EmiBundlePaths.resolve(tempDir, "items/minecraft/iron_pickaxe.json");
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
        RecipeBundleMods mods = RecipeIndexIds.writeFixtureLayout(
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

        Path tagFile = EmiBundlePaths.resolve(tempDir, "tags/tfc/items/glass_batches_tier_2.json");
        Files.createDirectories(tagFile.getParent());
        Files.writeString(tagFile, """
                {
                  "values": [
                    "tfc:silica_glass_batch",
                    "tfc:hematitic_glass_batch"
                  ]
                }
                """);

        EmiItemsIndexExporter.export(tempDir, null, mods);

        Path silicaFile = EmiBundlePaths.resolve(tempDir, "items/tfc/silica_glass_batch.json");
        Path hematiticFile = EmiBundlePaths.resolve(tempDir, "items/tfc/hematitic_glass_batch.json");
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
        RecipeBundleMods mods = RecipeIndexIds.writeFixtureLayout(
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
        mods = mergeMods(mods, RecipeIndexIds.writeFixtureLayout(
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

        EmiItemsIndexExporter.export(tempDir, null, mods);

        Path railFile = EmiBundlePaths.resolve(tempDir, "items/minecraft/activator_rail.json");
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

    private static JsonObject layout(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static RecipeBundleMods mergeMods(RecipeBundleMods first, RecipeBundleMods second) {
        RecipeBundleMods.Builder builder = RecipeBundleMods.builder();
        first.mods().forEach(builder::put);
        second.mods().forEach(builder::put);
        return builder.build();
    }
}
