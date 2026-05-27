package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconStackKeyTest {

    @Test
    void createsVariantKeyForNbtSnbt() {
        String key = IconStackKey.forItemIdAndNbtSnbt(
                ResourceLocation.parse("minecraft:potion"),
                "{Potion:\"minecraft:water\"}");

        assertTrue(key.startsWith("minecraft:potion@"));
        assertTrue(IconStackKey.isVariantKey(key));
    }

    @Test
    void leavesPlainIdsUntouchedWithoutNbt() {
        assertEquals(
                "minecraft:stick",
                IconStackKey.forItemIdAndNbtSnbt(
                        ResourceLocation.parse("minecraft:stick"),
                        ""));
        assertFalse(IconStackKey.isVariantKey("minecraft:stick"));
    }
}
