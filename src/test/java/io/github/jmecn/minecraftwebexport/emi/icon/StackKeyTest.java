package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.emi.icon.StackKey;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackKeyTest {

    @Test
    void createsVariantKeyForNbtSnbt() {
        String key = StackKey.forItemIdAndNbtSnbt(
                ResourceLocation.parse("minecraft:potion"),
                "{Potion:\"minecraft:water\"}");

        assertTrue(key.startsWith("minecraft:potion@"));
        assertTrue(StackKey.isVariantKey(key));
    }

    @Test
    void leavesPlainIdsUntouchedWithoutNbt() {
        assertEquals(
                "minecraft:stick",
                StackKey.forItemIdAndNbtSnbt(
                        ResourceLocation.parse("minecraft:stick"),
                        ""));
        assertFalse(StackKey.isVariantKey("minecraft:stick"));
    }
}
