package io.github.jmecn.minecraftwebexport.emi.icon;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.category.CategoryIconSprite;
import io.github.jmecn.minecraftwebexport.model.emi.icon.AtlasPagePlan;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasIndex;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasPage;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasSpriteEntry;
import java.util.Objects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

final class AtlasBuilder implements AutoCloseable {

    public static List<AtlasPagePlan> planPages(int spriteCount, int cellSize, int maxAtlasSize) {
        if (spriteCount <= 0) {
            return List.of(new AtlasPagePlan(1, 1));
        }
        int maxCols = Math.max(1, maxAtlasSize / cellSize);
        int maxRows = Math.max(1, maxAtlasSize / cellSize);
        int maxPerPage = maxCols * maxRows;

        List<AtlasPagePlan> pages = new ArrayList<>();
        int remaining = spriteCount;
        while (remaining > 0) {
            int onPage = Math.min(remaining, maxPerPage);
            int cols = (int) Math.ceil(Math.sqrt(onPage));
            int rows = (int) Math.ceil((double) onPage / cols);
            if (cols > maxCols) {
                cols = maxCols;
                rows = (int) Math.ceil((double) onPage / cols);
            }
            if (rows > maxRows) {
                rows = maxRows;
                cols = (int) Math.ceil((double) onPage / rows);
                cols = Math.min(cols, maxCols);
            }
            pages.add(new AtlasPagePlan(cols, rows));
            remaining -= cols * rows;
        }
        return pages;
    }

    public static int iconCellSize() {
        return resolveIconCellSize(
                MweConfig.iconSize(),
                MweConfig.itemIconSize(),
                MweConfig.blockItemIconSize(),
                MweConfig.fluidIconSize());
    }

    public static int resolveIconCellSize(int unified, int item, int block, int fluid) {
        if (unified > 0) {
            return boundedSize(unified, "icons.iconSize");
        }
        if (item > 0) {
            return boundedSize(item, "icons.itemIconSize");
        }
        if (block > 0) {
            return boundedSize(block, "icons.blockItemIconSize");
        }
        if (fluid > 0) {
            return boundedSize(fluid, "icons.fluidIconSize");
        }
        return Constants.DEFAULT_ICON_SIZE;
    }

    public static int categoryIconCellSize() {
        return boundedSize(MweConfig.categoryIconSize(), "icons.categoryIconSize");
    }

    public static int atlasMaxSize() {
        int size = MweConfig.itemIconAtlasMaxSize();
        if (size < 256 || size > 8192) {
            throw new IllegalArgumentException("icons.itemIconAtlasMaxSize must be 256..8192, got " + size);
        }
        return size;
    }

    public static void renderPlaceholder(GuiGraphics guiGraphics, OffScreenRenderer renderer) {
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

    private static int boundedSize(int size, String property) {
        if (size < 8 || size > 256) {
            throw new IllegalArgumentException(property + " must be 8..256, got " + size);
        }
        return size;
    }

    private final int cellSize;
    private final int maxAtlasSize;
    private final Path outputDir;
    private final String cssClass;
    private final String cssFileName;
    private final List<AtlasPagePlan> pagePlans;
    private final Map<String, Integer> usageWeights;

    private final ExportWriteQueue writes;
    private final List<PageInfo> pages = new ArrayList<>();
    private final Map<String, SpriteRef> items = new LinkedHashMap<>();

    private NativeImage currentPage;
    private int pageIndex;
    private int pageWidth;
    private int pageHeight;
    private int cursorX;
    private int cursorY;
    private int rowHeight;
    private int extentX;
    private int extentY;
    private int pagePlanIndex;

    private long pendingPngBytes;

    AtlasBuilder(
            Path outputDir,
            int cellSize,
            int maxAtlasSize,
            String atlasKind,
            List<AtlasPagePlan> pagePlans,
            Map<String, Integer> usageWeights,
            ExportWriteQueue writes) {
        this.writes = Objects.requireNonNull(writes, "writes");
        this.outputDir = outputDir;
        this.cellSize = cellSize;
        this.maxAtlasSize = maxAtlasSize;
        this.pagePlans = pagePlans != null ? List.copyOf(pagePlans) : List.of();
        this.usageWeights = usageWeights;
        if ("icon".equals(atlasKind)) {
            this.cssClass = "icon-atlas";
            this.cssFileName = "icons.css";
        } else {
            this.cssClass = atlasKind + "-icon-atlas";
            this.cssFileName = atlasKind + "-icons.css";
        }
        startNewPage();
    }

    CategoryIconSprite spriteFor(String itemId) {
        SpriteRef ref = items.get(itemId);
        if (ref == null) {
            return null;
        }
        return new CategoryIconSprite(ref.page(), ref.x(), ref.y());
    }

    void place(String itemId, OffScreenRenderer frame) {
        if (cursorX + cellSize > pageWidth) {
            nextRow();
        }
        if (cursorY + cellSize > pageHeight) {
            flushPageToDisk();
            startNewPage();
            if (cursorX + cellSize > pageWidth) {
                nextRow();
            }
        }

        frame.copyPixelsTo(currentPage, cursorX, cursorY);
        int usage = usageWeights != null ? usageWeights.getOrDefault(itemId, 0) : 0;
        items.put(itemId, new SpriteRef(pageIndex, cursorX, cursorY, usage));
        cursorX += cellSize;
        extentX = Math.max(extentX, cursorX);
        extentY = Math.max(extentY, cursorY + cellSize);
    }

    record AtlasResult(int itemsPlaced, int pageCount, long indexBytes, long pngBytes, long cssBytes) {
    }

    AtlasResult finish() throws IOException {
        if (currentPage != null && (!items.isEmpty() || cursorX > 0 || cursorY > 0)) {
            flushPageToDisk();
        } else if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }

        Files.createDirectories(outputDir);

        long pngBytes = pendingPngBytes;
        for (PageInfo page : pages) {
            Path out = outputDir.resolve(page.fileName());
            if (Files.exists(out)) {
                pngBytes += Files.size(out);
            }
        }

        List<AtlasPage> pageList = new ArrayList<>();
        for (PageInfo page : pages) {
            pageList.add(new AtlasPage(page.fileName(), page.width(), page.height()));
        }
        Map<String, AtlasSpriteEntry> spriteEntries = new LinkedHashMap<>();
        for (Map.Entry<String, SpriteRef> entry : items.entrySet()) {
            SpriteRef ref = entry.getValue();
            AtlasSpriteEntry sprite = usageWeights != null && !usageWeights.isEmpty()
                    ? new AtlasSpriteEntry(ref.page(), ref.x(), ref.y(), ref.usage())
                    : new AtlasSpriteEntry(ref.page(), ref.x(), ref.y());
            spriteEntries.put(entry.getKey(), sprite);
        }
        String sort = usageWeights != null && !usageWeights.isEmpty() ? "usageDesc" : null;
        AtlasIndex indexRoot = AtlasIndex.of(cellSize, sort, pageList, spriteEntries);

        Path indexPath = outputDir.resolve("index.json");
        writes.submitJson(indexPath, indexRoot);
        long indexJsonBytes = JsonIO.toUtf8Bytes(indexRoot).length;

        Path cssPath = outputDir.resolve(cssFileName);
        String css = buildCss();
        writes.submitString(cssPath, css);

        MweMod.LOGGER.info(
                "{} atlas: {} items, {} pages, index {} bytes, png {} bytes, css {} bytes",
                Log.ICONS,
                items.size(),
                pages.size(),
                indexJsonBytes,
                pngBytes,
                css.length());

        return new AtlasResult(items.size(), pages.size(), indexJsonBytes, pngBytes, css.length());
    }

    @Override
    public void close() {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }
        pages.clear();
    }

    private void nextRow() {
        extentX = Math.max(extentX, pageWidth);
        cursorX = 0;
        cursorY += rowHeight;
        rowHeight = cellSize;
    }

    private void startNewPage() {
        if (pagePlanIndex < pagePlans.size()) {
            AtlasPagePlan plan = pagePlans.get(pagePlanIndex);
            pageWidth = plan.widthPx(cellSize);
            pageHeight = plan.heightPx(cellSize);
            pagePlanIndex++;
        } else {
            pageWidth = maxAtlasSize;
            pageHeight = maxAtlasSize;
        }
        currentPage = new NativeImage(pageWidth, pageHeight, true);
        cursorX = 0;
        cursorY = 0;
        rowHeight = cellSize;
        extentX = 0;
        extentY = 0;
    }

    private void flushPageToDisk() {
        if (currentPage == null) {
            return;
        }
        int usedWidth = Math.max(extentX, cursorX);
        int usedHeight = Math.max(extentY, cursorY + (cursorX > 0 ? rowHeight : 0));
        if (usedWidth <= 0) {
            usedWidth = cellSize;
        }
        if (usedHeight <= 0) {
            usedHeight = cellSize;
        }
        usedWidth = Math.min(usedWidth, pageWidth);
        usedHeight = Math.min(usedHeight, pageHeight);

        String fileName = "atlas-%03d.png".formatted(pageIndex);
        NativeImage trimmed = cropToUsed(currentPage, usedWidth, usedHeight);
        currentPage.close();
        currentPage = null;
        try {
            byte[] png = trimmed.asByteArray();
            Path out = outputDir.resolve(fileName);
            writes.submitBytes(out, png);
            pendingPngBytes += png.length;
        } catch (IOException e) {
            throw new RuntimeException("failed to encode atlas page " + fileName, e);
        } finally {
            trimmed.close();
        }
        pages.add(new PageInfo(pageIndex, fileName, usedWidth, usedHeight));
        pageIndex++;
    }

    private static NativeImage cropToUsed(NativeImage source, int width, int height) {
        NativeImage cropped = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cropped.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return cropped;
    }

    private String buildCss() {
        StringBuilder sb = new StringBuilder();
        sb.append("/* Auto-generated item icon sprites. cell=").append(cellSize).append("px */\n");
        sb.append('.').append(cssClass).append(" {\n");
        sb.append("  width: ").append(cellSize).append("px;\n");
        sb.append("  height: ").append(cellSize).append("px;\n");
        sb.append("  image-rendering: pixelated;\n");
        sb.append("  image-rendering: crisp-edges;\n");
        sb.append("  background-repeat: no-repeat;\n");
        sb.append("  display: inline-block;\n");
        sb.append("  --cell: ").append(cellSize).append(";\n");
        sb.append("}\n\n");

        for (Map.Entry<String, SpriteRef> entry : items.entrySet()) {
            SpriteRef ref = entry.getValue();
            PageInfo page = pages.get(ref.page());
            sb.append('.').append(cssClass).append("[data-item=\"")
                    .append(escapeCssAttr(entry.getKey()))
                    .append("\"] {\n");
            sb.append("  --sprite-x: ").append(ref.x()).append(";\n");
            sb.append("  --sprite-y: ").append(ref.y()).append(";\n");
            sb.append("  --atlas-w: ").append(page.width()).append(";\n");
            sb.append("  --atlas-h: ").append(page.height()).append(";\n");
            sb.append("  background-image: url('")
                    .append(page.fileName())
                    .append("');\n");
            sb.append("  background-size: calc(var(--atlas-w) * 1px) calc(var(--atlas-h) * 1px);\n");
            sb.append("  background-position: calc(var(--sprite-x) * -1px) calc(var(--sprite-y) * -1px);\n");
            sb.append("}\n\n");
        }
        return sb.toString();
    }

    private static String escapeCssAttr(String id) {
        return id.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record SpriteRef(int page, int x, int y, int usage) {
    }

    private record PageInfo(int index, String fileName, int width, int height) {
    }
}
