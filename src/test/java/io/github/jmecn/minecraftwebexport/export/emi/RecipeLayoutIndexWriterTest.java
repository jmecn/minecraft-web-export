package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonArray;
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

class RecipeLayoutIndexWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesShardedRecipeIndex() throws IOException {
        List<String> recipeIds = List.of("minecraft:crafting_table", "emi:/foo/bar", "minecraft:yellow_bed");
        RecipeLayoutIndexWriter.write(tempDir, 2, recipeIds);

        Path indexFile = EmiBundlePaths.resolve(tempDir, RecipeLayoutPaths.LAYOUT_INDEX_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        JsonArray namespaces = json.getAsJsonArray("namespaces");
        Path minecraftShard = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.RECIPE_SHARDS_DIR + "/minecraft.json");
        Path emiShard = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.RECIPE_SHARDS_DIR + "/emi.json");
        JsonArray minecraftPaths = JsonParser.parseString(Files.readString(minecraftShard)).getAsJsonArray();
        JsonArray emiPaths = JsonParser.parseString(Files.readString(emiShard)).getAsJsonArray();

        assertTrue(Files.isRegularFile(indexFile));
        assertEquals(RecipeLayoutIndexWriter.INDEX_SCHEMA_VERSION, json.get("schema").getAsInt());
        assertEquals(2, json.get("scale").getAsInt());
        assertEquals(2, namespaces.size());
        assertEquals("emi", namespaces.get(0).getAsString());
        assertEquals("minecraft", namespaces.get(1).getAsString());
        assertEquals(1, emiPaths.size());
        assertEquals("/foo/bar", emiPaths.get(0).getAsString());
        assertEquals(2, minecraftPaths.size());
        assertEquals("crafting_table", minecraftPaths.get(0).getAsString());
        assertEquals("yellow_bed", minecraftPaths.get(1).getAsString());
        assertEquals(
                "recipes/layouts/emi__foo_bar.json",
                RecipeLayoutPaths.layoutPathForRecipeId("emi:/foo/bar"));
    }
}
