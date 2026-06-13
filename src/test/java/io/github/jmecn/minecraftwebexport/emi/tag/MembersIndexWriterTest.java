package io.github.jmecn.minecraftwebexport.emi.tag;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.tag.MembersIndexWriter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembersIndexWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesTagsCatalogWhenAnyTagExported() throws Exception {
        Set<String> items = new TreeSet<>(Set.of("forge:cloth", "minecraft:planks"));
        Set<String> blocks = new TreeSet<>(Set.of("minecraft:mineable/pickaxe"));

        long bytes = MembersIndexWriter.writeTagsCatalog(tempDir, items, blocks, Set.of());

        assertTrue(bytes > 0);
        Path indexFile = Paths.resolve(tempDir, Constants.TAGS_INDEX_FILE);
        assertTrue(Files.isRegularFile(indexFile));

        JsonObject root = JsonParser.parseString(Files.readString(indexFile)).getAsJsonObject();
        assertEquals(1, root.get("schema").getAsInt());
        assertEquals(2, root.getAsJsonArray("items").size());
        assertEquals("forge:cloth", root.getAsJsonArray("items").get(0).getAsString());
        assertEquals(1, root.getAsJsonArray("blocks").size());
        assertFalse(root.has("fluids"));
    }

    @Test
    void skipsTagsCatalogWhenNoTags() throws Exception {
        long bytes = MembersIndexWriter.writeTagsCatalog(tempDir, Set.of(), Set.of(), Set.of());
        assertEquals(0, bytes);
        assertFalse(Files.exists(Paths.resolve(tempDir, Constants.TAGS_INDEX_FILE)));
    }
}
