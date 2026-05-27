package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public final class WidgetChromeRasterizer {

    private WidgetChromeRasterizer() {
    }

    public record ChromeAsset(String exportPath, boolean deduplicated) {
    }

    public static ChromeAsset rasterizeWidget(
            Minecraft client,
            Widget widget,
            Path chromeRoot,
            Map<String, String> hashToRelative) throws IOException {
        Bounds bounds = widget.getBounds();
        if (bounds == null || bounds.empty()) {
            throw new IOException("widget bounds empty: " + widget.getClass().getName());
        }
        int width = Math.max(1, bounds.width());
        int height = Math.max(1, bounds.height());
        int scale = EmiRecipeLayoutExporter.layoutScale();
        int pixelWidth = width * scale;
        int pixelHeight = height * scale;

        try (OffScreenRenderer offScreen = new OffScreenRenderer(pixelWidth, pixelHeight)) {
            GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
            byte[] png = offScreen.captureAsPng(() -> offScreen.runWithEmiRecipeMatrices(width, height, () -> {
                graphics.pose().pushPose();
                graphics.pose().translate(-bounds.x(), -bounds.y(), 0);
                widget.render(graphics, -10_000, -10_000, 0);
                graphics.flush();
                graphics.pose().popPose();
            }));

            String hash = sha256(png);
            String relative = hashToRelative.get(hash);
            boolean deduped = relative != null;
            if (relative == null) {
                relative = "sh/" + hash.substring(0, 2) + "/" + hash + ".png";
                Path out = chromeRoot.resolve(relative);
                Files.createDirectories(out.getParent());
                Files.write(out, png);
                hashToRelative.put(hash, relative);
            }
            String exportPath = RecipeLayoutPaths.CHROME_DIR + "/" + relative.replace('\\', '/');
            return new ChromeAsset(exportPath, deduped);
        }
    }

    static boolean isRootWidget(Widget widget) {
        return widget.getClass().getName().endsWith("RootWidget");
    }

    static boolean isDrawableWidget(Widget widget) {
        if (widget instanceof dev.emi.emi.api.widget.DrawableWidget) {
            return true;
        }
        return widget.getClass().getName().endsWith(".DrawableWidget");
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
