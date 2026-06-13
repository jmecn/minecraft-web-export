package io.github.jmecn.minecraftwebexport.model.emi.icon;


public record CategoryIconResult(
        int categoryCount,
        int iconsPlaced,
        int iconFailures,
        long categoriesIndexBytes,
        long atlasIndexBytes) {
}
