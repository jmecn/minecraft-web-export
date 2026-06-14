package io.github.jmecn.minecraftwebexport.enums.tag;

public enum TagKind {
    ITEMS("items"),
    BLOCKS("blocks"),
    FLUIDS("fluids");

    private final String dirName;

    TagKind(String dirName) {
        this.dirName = dirName;
    }

    public String dirName() {
        return dirName;
    }
}
