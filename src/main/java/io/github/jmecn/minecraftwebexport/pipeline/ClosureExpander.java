package io.github.jmecn.minecraftwebexport.pipeline;

import net.minecraft.server.MinecraftServer;

import java.util.Set;
import java.util.TreeSet;

final class ClosureExpander {

    private ClosureExpander() {
    }

    static ClosureResult expand(MinecraftServer server, Seeds seeds) {
        Set<String> items = new TreeSet<>(seeds.itemIds());
        Set<String> fluids = new TreeSet<>(seeds.fluidIds());
        Set<String> tags = new TreeSet<>(seeds.tagIds());
        Set<String> blocks = new TreeSet<>(seeds.blockIds());

        if (server != null && !seeds.tagIds().isEmpty()) {
            io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander.Expansion expansion =
                    io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander.expand(server, seeds.tagIds());
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

    record ClosureResult(
            Set<String> itemIds,
            Set<String> blockIds,
            Set<String> fluidIds,
            Set<String> tagIds,
            Set<String> langKeys) {
    }
}
