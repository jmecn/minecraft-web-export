package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public final class FluidStillRenderer {

    private static final int GUI_SIZE = 16;

    private FluidStillRenderer() {
    }

    public static boolean render(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            Fluid fluid) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return false;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, 1000);
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null) {
            return false;
        }

        TextureAtlas atlas = client.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(still);

        int tint = extensions.getTintColor(stack);
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        if (a <= 0.0F) {
            a = 1.0F;
        }

        float fr = r;
        float fg = g;
        float fb = b;
        float fa = a;
        Runnable draw = () -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(fr, fg, fb, fa);
            guiGraphics.blit(0, 0, 0, GUI_SIZE, GUI_SIZE, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        };
        renderer.captureAsPng(draw);
        return true;
    }
}
