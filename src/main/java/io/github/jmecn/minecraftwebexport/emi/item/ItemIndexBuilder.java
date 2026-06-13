package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.model.emi.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

final class ItemIndexBuilder {

    private ItemIndexBuilder() {
    }

    static ItemIndexBuild buildFromContext(
            Path outputDir, MinecraftServer server, ExportContext context) throws IOException {
        ExportedTagCatalog exportedTags = ItemIndexSupport.loadExportedTagCatalog(outputDir);
        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(context.inputs().keySet());
        allItemIds.addAll(context.outputs().keySet());
        ItemIndexSupport.mergeSeedItemIds(allItemIds, context.itemIds());
        MweMod.LOGGER.info("{} resolving registry tags for {} items", Log.EMI_ITEMS, allItemIds.size());
        Map<String, RegistryTagSet> registryTagsByItem =
                ItemIndexSupport.resolveRegistryTags(allItemIds, server);
        return new ItemIndexBuild(
                context.inputs(),
                context.outputs(),
                Set.copyOf(context.fluidRegistryIds()),
                Set.copyOf(allItemIds),
                registryTagsByItem,
                exportedTags);
    }
}
