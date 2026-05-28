package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiItemsIndexExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsItemsIndexFromLayoutWidgets() throws IOException {
        RecipeLayoutIndexWriter.write(tempDir, 2, List.of("minecraft:iron_pickaxe"));

        Path layoutFile = EmiBundlePaths.resolve(
                tempDir,
                "recipes/layouts/minecraft_iron_pickaxe.json");
        Files.createDirectories(layoutFile.getParent());
        Files.writeString(layoutFile, """
                {
                  "schema": 2,
                  "id": "minecraft:iron_pickaxe",
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
                """);

        EmiItemsIndexExporter.Result result = EmiItemsIndexExporter.export(tempDir);

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
        assertEquals(1, stick.getAsJsonArray("inputs").size());
        assertEquals(1, pickaxe.getAsJsonArray("outputs").size());
    }

    @Test
    void expandsTagInputsUsingTagMembersIndex() throws IOException {
        RecipeLayoutIndexWriter.write(tempDir, 2, List.of("tfc:glassworking_jar"));

        Path layoutFile = EmiBundlePaths.resolve(
                tempDir,
                "recipes/layouts/tfc_glassworking_jar.json");
        Files.createDirectories(layoutFile.getParent());
        Files.writeString(layoutFile, """
                {
                  "schema": 2,
                  "id": "tfc:glassworking_jar",
                  "widgets": [
                    {
                      "type": "slot",
                      "role": "input",
                      "ingredient": "#item:tfc:glass_batches_tier_2",
                      "tagDisplayItem": "tfc:silica_glass_batch"
                    }
                  ]
                }
                """);

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

        EmiItemsIndexExporter.export(tempDir);

        Path silicaFile = EmiBundlePaths.resolve(tempDir, "items/tfc/silica_glass_batch.json");
        Path hematiticFile = EmiBundlePaths.resolve(tempDir, "items/tfc/hematitic_glass_batch.json");
        JsonObject hematitic = JsonParser.parseString(Files.readString(hematiticFile)).getAsJsonObject();

        assertTrue(Files.exists(silicaFile));
        assertTrue(Files.exists(hematiticFile));
        assertEquals(
                "tfc:glassworking_jar",
                hematitic.getAsJsonArray("inputs").get(0).getAsString());
    }

    @Test
    void skipsEmiTagDisplayRecipesInReverseIndex() throws IOException {
        RecipeLayoutIndexWriter.write(
                tempDir,
                2,
                List.of("emi:/tag/item/minecraft/rails", "minecraft:activator_rail"));

        Path tagLayout = EmiBundlePaths.resolve(
                tempDir,
                "recipes/layouts/emi__tag_item_minecraft_rails.json");
        Files.createDirectories(tagLayout.getParent());
        Files.writeString(tagLayout, """
                {
                  "schema": 2,
                  "id": "emi:/tag/item/minecraft/rails",
                  "widgets": [
                    {
                      "type": "slot",
                      "role": "input",
                      "tagDisplayItem": "minecraft:activator_rail"
                    }
                  ]
                }
                """);

        Path craftLayout = EmiBundlePaths.resolve(
                tempDir,
                "recipes/layouts/minecraft_activator_rail.json");
        Files.writeString(craftLayout, """
                {
                  "schema": 2,
                  "id": "minecraft:activator_rail",
                  "widgets": [
                    {
                      "type": "slot",
                      "role": "output",
                      "ingredient": "item:minecraft:activator_rail"
                    }
                  ]
                }
                """);

        EmiItemsIndexExporter.export(tempDir);

        Path railFile = EmiBundlePaths.resolve(tempDir, "items/minecraft/activator_rail.json");
        JsonObject rail = JsonParser.parseString(Files.readString(railFile)).getAsJsonObject();
        assertTrue(rail.has("outputs"));
        assertEquals(1, rail.getAsJsonArray("outputs").size());
        assertEquals("minecraft:activator_rail", rail.getAsJsonArray("outputs").get(0).getAsString());
        assertTrue(!rail.has("inputs") || rail.getAsJsonArray("inputs").isEmpty());
    }
}
