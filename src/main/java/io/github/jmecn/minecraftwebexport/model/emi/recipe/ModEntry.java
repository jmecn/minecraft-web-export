package io.github.jmecn.minecraftwebexport.model.emi.recipe;

import java.util.List;

public record ModEntry(List<String> routes, List<PackRef> packs) {
}
