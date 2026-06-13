package io.github.jmecn.minecraftwebexport.emi.recipe;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureWriterTest {

    @Test
    void mapsTextureIdToRelativePath() {
        assertEquals(
                "emi/textures/gui/widgets.png",
                TextureWriter.textureRelativePath(ResourceLocation.parse("emi:textures/gui/widgets.png")));
    }
}
