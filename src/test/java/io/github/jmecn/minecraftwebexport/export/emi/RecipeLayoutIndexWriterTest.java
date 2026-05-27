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

class RecipeLayoutIndexWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesCurrentRecipesIndexContract() throws IOException {
        RecipeLayoutIndexWriter.write(
                tempDir,
                2,
                Map.of(
                        "minecraft:crafting_table", new RecipeLayoutIndexWriter.Entry(
                                "recipes/layouts/minecraft_crafting_table.json",
                                "minecraft:crafting",
                                null)));

        Path indexFile = EmiBundlePaths.resolve(tempDir, RecipeLayoutPaths.LAYOUT_INDEX_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        JsonObject recipes = json.getAsJsonObject("recipes");
        JsonObject recipe = recipes.getAsJsonObject("minecraft:crafting_table");

        assertTrue(Files.isRegularFile(indexFile));
        assertEquals(2, json.get("schema").getAsInt());
        assertEquals(2, json.get("scale").getAsInt());
        assertEquals("recipes/layouts/minecraft_crafting_table.json", recipe.get("layout").getAsString());
        assertEquals("minecraft:crafting", recipe.get("category").getAsString());
    }
}
