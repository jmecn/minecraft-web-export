package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiBundleManifestWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesCurrentBundleContract() throws IOException {
        EmiBundleManifestWriter.write(tempDir, List.of("en_us", "zh_cn"), 2, 42);

        Path bundleFile = EmiBundlePaths.resolve(tempDir, EmiBundlePaths.BUNDLE_FILE);
        JsonObject json = JsonParser.parseString(Files.readString(bundleFile)).getAsJsonObject();

        assertTrue(Files.isRegularFile(bundleFile));
        assertEquals(2, json.get("schema").getAsInt());
        assertEquals(2, json.get("imageScale").getAsInt());
        assertFalse(json.has("mods"));
        assertFalse(json.has("layoutSchema"));
        assertEquals(2, json.getAsJsonArray("languages").size());
        assertEquals(42, json.get("recipeCount").getAsInt());
        assertEquals("png", json.get("recipeImageFormat").getAsString());
        assertEquals(IconPlaceholderRenderer.REGISTRY_ID, json.get("missingIconId").getAsString());
    }
}
