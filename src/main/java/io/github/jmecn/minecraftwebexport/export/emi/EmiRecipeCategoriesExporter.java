package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * EMI recipe category manifest + dedicated icon atlas ({@link EmiCategoryIconsExporter}).
 */
public final class EmiRecipeCategoriesExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiRecipeCategoriesExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private EmiRecipeCategoriesExporter() {
    }

    public record Result(int categoryCount, long indexBytes) {
    }

    public static Result export(Path outputRoot, net.minecraft.client.Minecraft client) throws IOException {
        EmiCategoryIconsExporter.Result detailed = EmiCategoryIconsExporter.export(outputRoot, client);
        return new Result(detailed.categoryCount(), detailed.categoriesIndexBytes());
    }

    static long writeCategoriesIndex(Path outputRoot, Map<String, Object> root) throws IOException {
        Path indexFile = EmiBundlePaths.resolve(outputRoot, EmiBundlePaths.CATEGORIES_INDEX_FILE);
        Files.createDirectories(indexFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(indexFile, json);
        LOGGER.info("{} categories index -> {}", ExportLog.EMI, indexFile);
        return json.length();
    }
}
