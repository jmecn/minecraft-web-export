package io.github.jmecn.minecraftwebexport.emi.tag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
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
        Path indexFile = EmiPaths.resolve(tempDir, Constants.TAGS_INDEX_FILE);
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
        assertFalse(Files.exists(EmiPaths.resolve(tempDir, Constants.TAGS_INDEX_FILE)));
    }

    @Test
    void filterRedundantFlowingFluidsDropsFlowingWhenStillPresent() {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("minecraft:water");
        values.add("minecraft:flowing_water");
        values.add("minecraft:lava");
        values.add("minecraft:flowing_lava");

        Set<String> filtered = MembersIndexWriter.filterRedundantFlowingFluids(values);

        assertEquals(Set.of("minecraft:water", "minecraft:lava"), filtered);
    }

    @Test
    void filterRedundantFlowingFluidsKeepsFlowingWhenStillMissing() {
        Set<String> filtered = MembersIndexWriter.filterRedundantFlowingFluids(
                Set.of("minecraft:flowing_water"));

        assertEquals(Set.of("minecraft:flowing_water"), filtered);
    }
}
