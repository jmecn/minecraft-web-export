package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.data.EmiRecipeCategoryProperties;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmiRecipeCategoriesExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiRecipeCategoriesExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private EmiRecipeCategoriesExporter() {
    }

    public record Result(int categoryCount, long indexBytes) {
    }

    public static Result export(Path outputDir) throws IOException {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            LOGGER.warn("{} EMI recipe manager unavailable - skipping categories index", ExportLog.EMI);
            return new Result(0, 0);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        List<EmiRecipeCategory> registered = manager.getCategories();
        int index = 0;
        for (EmiRecipeCategory category : registered) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", category.getId().toString());
            entry.put("order", index++);
            int priority = EmiRecipeCategoryProperties.getOrder(category);
            if (priority != 0) {
                entry.put("priority", priority);
            }
            entry.put("nameKey", EmiUtil.translateId("emi.category.", category.getId()));

            EmiRenderable icon = EmiRecipeCategoryProperties.getIcon(category);
            String iconKey = iconKeyForRenderable(icon);
            if (iconKey != null) {
                entry.put("iconKey", iconKey);
            }
            String iconItem = iconItemForRenderable(icon);
            if (iconItem != null) {
                entry.put("iconItem", iconItem);
            }
            categories.add(entry);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("categories", categories);

        Path indexFile = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.CATEGORIES_INDEX_FILE);
        Files.createDirectories(indexFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(indexFile, json);
        LOGGER.info("{} {} categories -> {}", ExportLog.EMI, categories.size(), indexFile);
        return new Result(categories.size(), json.length());
    }

    private static String iconKeyForRenderable(EmiRenderable renderable) {
        if (renderable instanceof EmiStack stack) {
            return IconStackKey.forEmiStack(stack);
        }
        return null;
    }

    private static String iconItemForRenderable(EmiRenderable renderable) {
        if (renderable instanceof EmiStack stack) {
            return IconStackKey.itemIdForEmiStack(stack);
        }
        return null;
    }
}
