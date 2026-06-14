package io.github.jmecn.minecraftwebexport.emi.tag;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagIndexWriterTest {

    @Test
    void filterTagMembersDropsHiddenIds() {
        Set<String> values = new LinkedHashSet<>();
        values.add("minecraft:oak_log");
        values.add("tfg:glacian_log");
        values.add("minecraft:spruce_log");

        Set<String> filtered = TagIndexWriter.filterTagMembers(values, id -> !"tfg:glacian_log".equals(id));

        assertEquals(Set.of("minecraft:oak_log", "minecraft:spruce_log"), filtered);
    }

    @Test
    void filterTagMembersPreservesOrder() {
        Set<String> values = new LinkedHashSet<>();
        values.add("a:first");
        values.add("b:second");
        values.add("c:third");

        Set<String> filtered = TagIndexWriter.filterTagMembers(values, id -> !"b:second".equals(id));

        assertEquals(2, filtered.size());
        assertTrue(filtered instanceof LinkedHashSet);
        String[] ordered = filtered.toArray(String[]::new);
        assertEquals("a:first", ordered[0]);
        assertEquals("c:third", ordered[1]);
    }

    @Test
    void filterTagMembersReturnsEmptyWhenAllHidden() {
        Set<String> values = Set.of("tfg:glacian_log");

        Set<String> filtered = TagIndexWriter.filterTagMembers(values, id -> false);

        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterRedundantFlowingFluidsDropsFlowingWhenStillPresent() {
        Set<String> values = new LinkedHashSet<>();
        values.add("minecraft:water");
        values.add("minecraft:flowing_water");

        Set<String> filtered = TagIndexWriter.filterRedundantFlowingFluids(values);

        assertEquals(Set.of("minecraft:water"), filtered);
    }

    @Test
    void filterRedundantFlowingFluidsKeepsOrphanFlowing() {
        Set<String> values = Set.of("minecraft:flowing_water");

        Set<String> filtered = TagIndexWriter.filterRedundantFlowingFluids(values);

        assertEquals(Set.of("minecraft:flowing_water"), filtered);
    }
}
