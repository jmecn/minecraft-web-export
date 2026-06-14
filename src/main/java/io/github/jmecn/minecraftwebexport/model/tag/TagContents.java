package io.github.jmecn.minecraftwebexport.model.tag;

import java.util.Set;

public record TagContents(Set<String> items, Set<String> blocks, Set<String> fluids) {
}
