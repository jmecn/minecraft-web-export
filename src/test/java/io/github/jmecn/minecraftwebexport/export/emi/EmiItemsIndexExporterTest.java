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
}
