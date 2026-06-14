package io.github.jmecn.minecraftwebexport.model.item;


public record NameKeysResult(int itemCount, int fluidCount) {

    public static final NameKeysResult EMPTY = new NameKeysResult(0, 0);
}
