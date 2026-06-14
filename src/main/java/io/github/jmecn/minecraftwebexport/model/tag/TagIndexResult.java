package io.github.jmecn.minecraftwebexport.model.tag;

import java.util.Set;

public record TagIndexResult(
        int tagsIndexed,
        int itemTagEntries,
        int blockTagEntries,
        int fluidTagEntries,
        int totalRegistryRefs,
        long tagFileBytes,
        long catalogIndexBytes,
        Set<String> itemTags,
        Set<String> blockTags,
        Set<String> fluidTags) {
}
