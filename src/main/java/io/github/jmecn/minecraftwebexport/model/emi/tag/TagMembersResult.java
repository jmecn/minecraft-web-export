package io.github.jmecn.minecraftwebexport.model.emi.tag;

import java.util.Set;

public record TagMembersResult(
        int tagsIndexed,
        int itemTagEntries,
        int blockTagEntries,
        int fluidTagEntries,
        int totalMemberRefs,
        long tagFileBytes,
        long catalogIndexBytes,
        Set<String> itemTags,
        Set<String> blockTags,
        Set<String> fluidTags) {
}
