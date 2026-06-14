package io.github.jmecn.minecraftwebexport.model.recipe;


public record TextureWriteResult(int requested, int written, int missing, long pngBytes){
    }
