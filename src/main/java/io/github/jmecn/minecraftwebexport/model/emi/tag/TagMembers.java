package io.github.jmecn.minecraftwebexport.model.emi.tag;

import java.util.Set;

public record TagMembers(Set<String> items, Set<String> blocks, Set<String> fluids) {
}
