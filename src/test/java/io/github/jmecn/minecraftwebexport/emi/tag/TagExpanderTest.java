package io.github.jmecn.minecraftwebexport.emi.tag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TagExpanderTest {

    @Test
    void normalizeTagRefStripsHashPrefix() {
        assertEquals("forge:ingots/iron", TagExpander.normalizeTagRef("#forge:ingots/iron"));
        assertEquals("minecraft:planks", TagExpander.normalizeTagRef("minecraft:planks"));
    }

    @Test
    void normalizeTagRefReturnsNullForBlank() {
        assertNull(TagExpander.normalizeTagRef(" "));
    }
}
