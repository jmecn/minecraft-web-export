package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.ModEntry;
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
        ModEntry minecraft = mods.mods().get("minecraft");
        ModEntry emi = mods.mods().get("emi");
        assertEquals(1, minecraft.routes().size());
        assertEquals(1, emi.routes().size());
        assertEquals(1, minecraft.packs().size());
        assertEquals(1, emi.packs().size());

        Path minecraftRoute = EmiPaths.resolve(
                tempDir,
                Constants.RECIPES_ROUTES_DIR + "/minecraft/" + minecraft.routes().get(0) + ".json");
        Path emiRoute = EmiPaths.resolve(
                tempDir,
                Constants.RECIPES_ROUTES_DIR + "/emi/" + emi.routes().get(0) + ".json");
        assertTrue(Files.isRegularFile(minecraftRoute));
        assertTrue(Files.isRegularFile(emiRoute));

        JsonObject minecraftRoutes = JsonParser.parseString(Files.readString(minecraftRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        JsonObject emiRoutes = JsonParser.parseString(Files.readString(emiRoute)).getAsJsonObject()
                .getAsJsonObject("routes");
        assertEquals(0, minecraftRoutes.get("crafting_table").getAsInt());
        assertEquals(0, minecraftRoutes.get("yellow_bed").getAsInt());
        assertEquals(0, emiRoutes.get("/foo/bar").getAsInt());

        Path minecraftPack = EmiPaths.resolve(
                tempDir,
                Constants.RECIPES_LAYOUT_PACKS_DIR + "/minecraft/" + minecraft.packs().get(0).file() + ".json");
        JsonObject packBody = JsonParser.parseString(Files.readString(minecraftPack)).getAsJsonObject();
        assertFalse(packBody.has("id"), "layout pack must not contain top-level id");

        List<String> ids = IndexIds.allRecipeIds(tempDir, mods);
        assertEquals(3, ids.size());
    }

    private static JsonObject layout(String id) {
        JsonObject layout = new JsonObject();
        layout.addProperty("schema", Constants.LAYOUT_PACK_SCHEMA);
        layout.addProperty("id", id);
        layout.add("widgets", new JsonArray());
        return layout;
    }
}
