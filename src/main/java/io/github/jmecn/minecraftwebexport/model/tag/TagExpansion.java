package io.github.jmecn.minecraftwebexport.model.tag;

import java.util.Set;

public record TagExpansion(
        Set<String> items,
        Set<String> blocks,
        Set<String> fluids,
        Set<String> tags) {
}
