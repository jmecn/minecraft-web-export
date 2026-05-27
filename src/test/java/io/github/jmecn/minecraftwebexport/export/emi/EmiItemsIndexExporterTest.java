package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiItemsIndexExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsItemsIndexFromLayoutWidgets() throws IOException {
        RecipeLayoutIndexWriter.write(
                tempDir,
                2,
                Map.of(
                        "minecraft:iron_pickaxe", new RecipeLayoutIndexWriter.Entry(
                                "emi/recipes/layouts/minecraft_iron_pickaxe.json",
                                "minecraft:crafting",
                                null)));

        Path layoutFile = EmiBundlePaths.resolve(
                tempDir,
                "emi/recipes/layouts/minecraft_iron_pickaxe.json");
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
        JsonObject items = json.getAsJsonObject("items");

        assertEquals(1, json.get("schema").getAsInt());
        assertEquals(3, json.get("itemCount").getAsInt());
        assertEquals(1, items.getAsJsonObject("minecraft:stick").getAsJsonArray("inputs").size());
        assertEquals(1, items.getAsJsonObject("minecraft:iron_pickaxe").getAsJsonArray("outputs").size());
    }

    @Test
    void expandsTagInputsUsingTagMembersIndex() throws IOException {
        RecipeLayoutIndexWriter.write(
                tempDir,
                2,
                Map.of(
                        "tfc:glassworking_jar", new RecipeLayoutIndexWriter.Entry(
                                "emi/recipes/layouts/tfc_glassworking_jar.json",
                                "tfc:glassworking",
                                null)));

        Path layoutFile = EmiBundlePaths.resolve(
                tempDir,
                "emi/recipes/layouts/tfc_glassworking_jar.json");
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

        Path tagMembersFile = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.TAG_MEMBERS_FILE);
        Files.createDirectories(tagMembersFile.getParent());
        Files.writeString(tagMembersFile, """
                {
                  "schema": 1,
                  "items": {
                    "tfc:glass_batches_tier_2": [
                      "tfc:silica_glass_batch",
                      "tfc:hematitic_glass_batch"
                    ]
                  }
                }
                """);

        EmiItemsIndexExporter.export(tempDir);

        Path itemsIndexFile = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.ITEMS_INDEX_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(itemsIndexFile)).getAsJsonObject();
        JsonObject items = json.getAsJsonObject("items");

        assertTrue(items.has("tfc:silica_glass_batch"));
        assertTrue(items.has("tfc:hematitic_glass_batch"));
        assertEquals(
                "tfc:glassworking_jar",
                items.getAsJsonObject("tfc:hematitic_glass_batch")
                        .getAsJsonArray("inputs")
                        .get(0)
                        .getAsString());
    }
}
