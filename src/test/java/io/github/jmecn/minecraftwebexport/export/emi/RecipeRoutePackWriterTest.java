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

class RecipeRoutePackWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesRoutesAndLayoutPacks() throws IOException {
        RecipeRoutePackWriter writer = new RecipeRoutePackWriter(tempDir, 262144);
        writer.addLayout("minecraft:crafting_table", layout("minecraft:crafting_table"));
        writer.addLayout("emi:/foo/bar", layout("emi:/foo/bar"));
        writer.addLayout("minecraft:yellow_bed", layout("minecraft:yellow_bed"));
        RecipeBundleMods mods = writer.finish();

        assertEquals(2, mods.mods().size());
        RecipeBundleMods.ModEntry minecraft = mods.mods().get("minecraft");
        RecipeBundleMods.ModEntry emi = mods.mods().get("emi");
        assertEquals(1, minecraft.routes().size());
        assertEquals(1, emi.routes().size());
        assertEquals(1, minecraft.packs().size());
        assertEquals(1, emi.packs().size());

        Path minecraftRoute = EmiBundlePaths.resolve(
                tempDir,
                EmiBundlePaths.RECIPES_ROUTES_DIR + "/minecraft/" + minecraft.routes().get(0) + ".json");
        Path emiRoute = EmiBundlePaths.resolve(
                tempDir,
                EmiBundlePaths.RECIPES_ROUTES_DIR + "/emi/" + emi.routes().get(0) + ".json");
        assertTrue(Files.isRegularFile(minecraftRoute));
        assertTrue(Files.isRegularFile(emiRoute));

        JsonObject minecraftRoutes = JsonParser.parseString(Files.readString(minecraftRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        JsonObject emiRoutes = JsonParser.parseString(Files.readString(emiRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        assertEquals(0, minecraftRoutes.get("crafting_table").getAsInt());
        assertEquals(0, minecraftRoutes.get("yellow_bed").getAsInt());
        assertEquals(0, emiRoutes.get("/foo/bar").getAsInt());

        List<String> ids = RecipeIndexIds.allRecipeIds(tempDir, mods);
        assertEquals(3, ids.size());
    }

    private static JsonObject layout(String id) {
        JsonObject layout = new JsonObject();
        layout.addProperty("schema", RecipeLayoutPaths.SCHEMA_VERSION);
        layout.addProperty("id", id);
        layout.add("widgets", new JsonArray());
        return layout;
    }
}
