package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class IconPlaceholderRenderer {

    public static final String REGISTRY_ID = "minecraft_web_export:missing_icon";

    private static final int MAGENTA = 0xFFFF00FF;
    private static final int BLACK = 0xFF000000;

    private IconPlaceholderRenderer() {
    }

    public static void render(Minecraft client, GuiGraphics guiGraphics, OffScreenRenderer renderer) {
        Runnable draw = () -> {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int color = ((x / 4) + (y / 4)) % 2 == 0 ? MAGENTA : BLACK;
                    guiGraphics.fill(x, y, x + 1, y + 1, color);
                }
            }
            guiGraphics.drawString(client.font, "?", 6, 4, 0xFFFFFFFF, false);
        };
        renderer.setupFlatGuiRendering();
        renderer.captureAsPng(draw);
    }
}
