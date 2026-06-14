package io.github.jmecn.minecraftwebexport.model.bundle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleTest {

    @TempDir
    Path tempDir;

    @Test
    void writesCurrentBundleContract() throws IOException {
        Bundle bundle = Bundle.of(2, 42, List.of("en_us", "zh_cn"), Constants.MISSING_ICON_REGISTRY_ID, null);
        JsonIO.write(EmiPaths.resolve(tempDir, Constants.BUNDLE_FILE), bundle);

        Path bundleFile = EmiPaths.resolve(tempDir, Constants.BUNDLE_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(bundleFile)).getAsJsonObject();

        assertTrue(Files.isRegularFile(bundleFile));
        assertEquals(2, json.get("schema").getAsInt());
        assertEquals(2, json.get("imageScale").getAsInt());
        assertFalse(json.has("mods"));
        assertFalse(json.has("layoutSchema"));
        assertEquals(2, json.getAsJsonArray("languages").size());
        assertEquals(42, json.get("recipeCount").getAsInt());
        assertEquals("png", json.get("recipeImageFormat").getAsString());
        assertEquals(Constants.MISSING_ICON_REGISTRY_ID, json.get("missingIconId").getAsString());
    }
}
