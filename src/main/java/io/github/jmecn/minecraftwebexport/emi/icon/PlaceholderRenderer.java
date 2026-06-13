package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;

import net.minecraft.client.gui.GuiGraphics;

public final class PlaceholderRenderer {

    public static final String REGISTRY_ID = Constants.MISSING_ICON_REGISTRY_ID;

    private PlaceholderRenderer() {}

    public static void render(GuiGraphics guiGraphics, OffScreenRenderer renderer) {
        Runnable draw = () -> {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int color = ((x / 8) + (y / 8)) % 2 == 0
                            ? Constants.PLACEHOLDER_MAGENTA
                            : Constants.PLACEHOLDER_BLACK;
                    guiGraphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        };
        renderer.setupFlatGuiRendering();
        renderer.captureAsPng(draw);
    }
}
