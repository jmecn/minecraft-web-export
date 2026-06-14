package io.github.jmecn.minecraftwebexport.model.tag;

public record TagWriteOutcome(boolean written, int registryRefs, int bytes) {

    public static final TagWriteOutcome EMPTY = new TagWriteOutcome(false, 0, 0);
}
