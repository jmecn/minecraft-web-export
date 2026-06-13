package io.github.jmecn.minecraftwebexport.emi.category;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.Json;
import io.github.jmecn.minecraftwebexport.model.emi.category.CategoryIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.CategoryIconResult;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import io.github.jmecn.minecraftwebexport.MweMod;

public final class IndexWriter {

    private IndexWriter() {
    }


    public static CategoryIndexResult export(Path outputRoot, net.minecraft.client.Minecraft client) throws IOException {
        CategoryIconResult detailed = CategoryIconWriter.export(outputRoot, client);
        return new CategoryIndexResult(detailed.categoryCount(), detailed.categoriesIndexBytes());
    }

    public static long writeCategoriesIndex(Path outputRoot, Map<String, Object> root) throws IOException {
        Path indexFile = Paths.resolve(outputRoot, Constants.CATEGORIES_INDEX_FILE);
        Files.createDirectories(indexFile.getParent());
        String json = Json.GSON.toJson(root);
        Files.writeString(indexFile, json);
        MweMod.LOGGER.info("{} categories index -> {}", Log.EMI, indexFile);
        return json.length();
    }
}
