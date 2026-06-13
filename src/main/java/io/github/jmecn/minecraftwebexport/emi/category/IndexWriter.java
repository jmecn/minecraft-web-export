package io.github.jmecn.minecraftwebexport.emi.category;

import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.model.emi.category.CategoryIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.CategoryIconResult;

import java.io.IOException;
import java.nio.file.Path;

public final class IndexWriter {

    private IndexWriter() {
    }

    public static CategoryIndexResult export(Path outputRoot, net.minecraft.client.Minecraft client) throws IOException {
        CategoryIconResult detailed = CategoryIconWriter.export(outputRoot, client);
        return new CategoryIndexResult(detailed.categoryCount(), detailed.categoriesIndexBytes());
    }
}
