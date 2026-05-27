package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TagClosureExpanderTest {

    @Test
    void normalizesHashPrefixedTagRefs() {
        assertEquals("forge:ingots/iron", TagClosureExpander.normalizeTagRef("#forge:ingots/iron"));
        assertEquals("minecraft:planks", TagClosureExpander.normalizeTagRef("minecraft:planks"));
    }

    @Test
    void returnsNullForBlankRefs() {
        assertNull(TagClosureExpander.normalizeTagRef(" "));
    }
}
