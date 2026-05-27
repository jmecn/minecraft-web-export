package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeTextureExporterTest {

    @Test
    void mapsTextureIdToRelativePath() {
        assertEquals(
                "emi/textures/gui/widgets.png",
                RecipeTextureExporter.textureRelativePath(ResourceLocation.parse("emi:textures/gui/widgets.png")));
    }
}
