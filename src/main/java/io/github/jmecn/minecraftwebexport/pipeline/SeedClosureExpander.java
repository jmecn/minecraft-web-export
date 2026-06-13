package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagExpansion;
import io.github.jmecn.minecraftwebexport.model.pipeline.ClosureResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

final class SeedClosureExpander {

    private SeedClosureExpander() {
    }

    static ClosureResult expand(MinecraftServer server, Seeds seeds) {
        Set<String> items = new TreeSet<>(seeds.itemIds());
        Set<String> fluids = new TreeSet<>(seeds.fluidIds());
        Set<String> tags = new TreeSet<>(seeds.tagIds());
        Set<String> blocks = new TreeSet<>(seeds.blockIds());

        if (server != null && !seeds.tagIds().isEmpty()) {
            TagExpansion expansion = ClosureExpander.expand(server, seeds.tagIds());
            items.addAll(expansion.items());
            fluids.addAll(expansion.fluids());
            blocks.addAll(expansion.blocks());
            tags.addAll(expansion.tags());
        }

        return new ClosureResult(
                Set.copyOf(items),
                Set.copyOf(blocks),
                Set.copyOf(fluids),
                Set.copyOf(tags),
                Set.copyOf(seeds.langKeys()));
    }
}
