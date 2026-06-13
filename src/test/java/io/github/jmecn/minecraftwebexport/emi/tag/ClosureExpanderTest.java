package io.github.jmecn.minecraftwebexport.emi.tag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClosureExpanderTest {

    @Test
    void normalizesHashPrefixedTagRefs() {
        assertEquals("forge:ingots/iron", ClosureExpander.normalizeTagRef("#forge:ingots/iron"));
        assertEquals("minecraft:planks", ClosureExpander.normalizeTagRef("minecraft:planks"));
    }

    @Test
    void returnsNullForBlankRefs() {
        assertNull(ClosureExpander.normalizeTagRef(" "));
    }
}
