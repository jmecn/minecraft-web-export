package io.github.jmecn.minecraftwebexport.model.item;


public record ItemIndexResult(
        int itemCount,
        int inputsIndexed,
        int outputsIndexed,
        long indexBytes) {
}
