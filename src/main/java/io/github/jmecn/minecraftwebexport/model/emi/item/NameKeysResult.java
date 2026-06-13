package io.github.jmecn.minecraftwebexport.model.emi.item;


public record NameKeysResult(int itemCount, int fluidCount) {

    public static final NameKeysResult EMPTY = new NameKeysResult(0, 0);
}
