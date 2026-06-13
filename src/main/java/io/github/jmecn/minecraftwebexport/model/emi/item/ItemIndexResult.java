package io.github.jmecn.minecraftwebexport.model.emi.item;


public record ItemIndexResult(
        int itemCount,
        int inputsIndexed,
        int outputsIndexed,
        long indexBytes) {
}
