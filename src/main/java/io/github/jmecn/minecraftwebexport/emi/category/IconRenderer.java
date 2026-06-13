package io.github.jmecn.minecraftwebexport.emi.category;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;
import io.github.jmecn.minecraftwebexport.emi.icon.StackKey;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.data.EmiRecipeCategoryProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class IconRenderer {

    private IconRenderer() {
    }

    public static boolean render(Minecraft client, GuiGraphics guiGraphics, OffScreenRenderer renderer, EmiRecipeCategory category) {
        EmiRenderable icon = EmiRecipeCategoryProperties.getIcon(category);
        if (renderRenderable(client, guiGraphics, renderer, icon)) {
            return true;
        }
        if (icon instanceof EmiStack stack) {
            ItemStack item = StackKey.toItemStack(stack);
            if (!item.isEmpty()) {
                return renderItemStack(client, guiGraphics, renderer, item);
            }
        }
        return false;
    }

    private static boolean renderRenderable(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            EmiRenderable renderable) {
        if (renderable == null) {
            return false;
        }
        try {
            renderer.setupFlatGuiRendering();
            Runnable draw = () -> renderable.render(guiGraphics, 0, 0, 0);
            renderer.captureAsPng(draw);
            return true;
        } catch (RuntimeException first) {
            if (renderable instanceof EmiStack stack) {
                ItemStack item = StackKey.toItemStack(stack);
                if (!item.isEmpty()) {
                    return renderItemStack(client, guiGraphics, renderer, item);
                }
            }
            return false;
        }
    }

    public static boolean renderItemStack(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            ItemStack stack) {
        try {
            renderer.setupItemRendering();
            Runnable draw = () -> {
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
            };
            renderer.captureAsPng(draw);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
