package io.github.jmecn.minecraftwebexport.emi.recipe;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import io.github.jmecn.minecraftwebexport.emi.recipe.IndexIds;
import io.github.jmecn.minecraftwebexport.emi.recipe.LayoutPaths;
import io.github.jmecn.minecraftwebexport.emi.recipe.RoutePackWriter;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePackWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesRoutesAndLayoutPacks() throws IOException {
        RoutePackWriter writer = new RoutePackWriter(tempDir, 262144);
        writer.addLayout("minecraft:crafting_table", layout("minecraft:crafting_table"));
        writer.addLayout("emi:/foo/bar", layout("emi:/foo/bar"));
        writer.addLayout("minecraft:yellow_bed", layout("minecraft:yellow_bed"));
        BundleMods mods = writer.finish();

        assertEquals(2, mods.mods().size());
        BundleMods.ModEntry minecraft = mods.mods().get("minecraft");
        BundleMods.ModEntry emi = mods.mods().get("emi");
        assertEquals(1, minecraft.routes().size());
        assertEquals(1, emi.routes().size());
        assertEquals(1, minecraft.packs().size());
        assertEquals(1, emi.packs().size());

        Path minecraftRoute = Paths.resolve(
                tempDir,
                Paths.RECIPES_ROUTES_DIR + "/minecraft/" + minecraft.routes().get(0) + ".json");
        Path emiRoute = Paths.resolve(
                tempDir,
                Paths.RECIPES_ROUTES_DIR + "/emi/" + emi.routes().get(0) + ".json");
        assertTrue(Files.isRegularFile(minecraftRoute));
        assertTrue(Files.isRegularFile(emiRoute));

        JsonObject minecraftRoutes = JsonParser.parseString(Files.readString(minecraftRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        JsonObject emiRoutes = JsonParser.parseString(Files.readString(emiRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        assertEquals(0, minecraftRoutes.get("crafting_table").getAsInt());
        assertEquals(0, minecraftRoutes.get("yellow_bed").getAsInt());
        assertEquals(0, emiRoutes.get("/foo/bar").getAsInt());

        Path minecraftPack = Paths.resolve(
                tempDir,
                Paths.RECIPES_LAYOUT_PACKS_DIR + "/minecraft/" + minecraft.packs().get(0).file() + ".json");
        JsonObject packBody = JsonParser.parseString(Files.readString(minecraftPack)).getAsJsonObject();
        assertFalse(packBody.has("id"), "layout pack must not contain top-level id");

        List<String> ids = IndexIds.allRecipeIds(tempDir, mods);
        assertEquals(3, ids.size());
    }

    private static JsonObject layout(String id) {
        JsonObject layout = new JsonObject();
        layout.addProperty("schema", LayoutPaths.SCHEMA_VERSION);
        layout.addProperty("id", id);
        layout.add("widgets", new JsonArray());
        return layout;
    }
}
