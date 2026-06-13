package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.Set;

public record ClosureResult(
        Set<String> itemIds,
        Set<String> blockIds,
        Set<String> fluidIds,
        Set<String> tagIds,
        Set<String> langKeys) {
}
