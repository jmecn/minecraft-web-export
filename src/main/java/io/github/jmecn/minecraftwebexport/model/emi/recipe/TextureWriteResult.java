package io.github.jmecn.minecraftwebexport.model.emi.recipe;


public record TextureWriteResult(int requested, int written, int missing, long pngBytes){
    }
