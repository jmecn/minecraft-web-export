package io.github.jmecn.minecraftwebexport.emi.category;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class IndexWriter {
    private static final com.google.gson.Gson GSON = io.github.jmecn.minecraftwebexport.emi.bundle.Gson.GSON;

    private IndexWriter() {
    }

    public record Result(int categoryCount, long indexBytes) {
    }

    public static Result export(Path outputRoot, net.minecraft.client.Minecraft client) throws IOException {
        CategoryIconWriter.Result detailed = CategoryIconWriter.export(outputRoot, client);
        return new Result(detailed.categoryCount(), detailed.categoriesIndexBytes());
    }

    public static long writeCategoriesIndex(Path outputRoot, Map<String, Object> root) throws IOException {
        Path indexFile = Paths.resolve(outputRoot, Paths.CATEGORIES_INDEX_FILE);
        Files.createDirectories(indexFile.getParent());
        String json = GSON.toJson(root);
        Files.writeString(indexFile, json);
        MinecraftWebExportMod.LOGGER.info("{} categories index -> {}", Log.EMI, indexFile);
        return json.length();
    }
}
