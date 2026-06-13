package io.github.jmecn.minecraftwebexport.model.emi.item;

import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import java.util.Map;
import java.util.Set;

public record ItemIndexBuild(
        Map<String, Map<String, Set<String>>> inputs,
        Map<String, Map<String, Set<String>>> outputs,
        Set<String> fluidRegistryIds,
        Set<String> allItemIds,
        Map<String, RegistryTagSet> registryTagsByItem,
        ExportedTagCatalog exportedTags) {
}
