package io.github.jmecn.minecraftwebexport.emi.pipeline;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;

public final class Readiness {

    private Readiness() {}

    public static boolean isReloadFailed() {
        return EmiReloadManager.getStatus() == -1;
    }

    public static boolean isReloadInProgress() {
        int status = EmiReloadManager.getStatus();
        return status == 1 || (status == 2 && !EmiReloadManager.isLoaded());
    }

    public static String reloadStatusLabel() {
        return switch (EmiReloadManager.getStatus()) {
            case -1 -> "error";
            case 0 -> "idle";
            case 1 -> "reloading";
            case 2 -> EmiReloadManager.isLoaded() ? "loaded" : "finishing";
            default -> "unknown(" + EmiReloadManager.getStatus() + ")";
        };
    }

    public static boolean isReadyForExport(Minecraft client) {
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
            return false;
        }
        if (!EmiReloadManager.isLoaded()) {
            return false;
        }
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        return manager != null && !manager.getRecipes().isEmpty();
    }
}
