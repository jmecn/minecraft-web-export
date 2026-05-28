package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;

/**
 * Gates export on EMI's own reload lifecycle, not merely a non-empty recipe list.
 *
 * <p>During {@code EmiRecipes.bake()}, EMI assigns an interim {@link dev.emi.emi.api.recipe.EmiRecipeManager}
 * while a background worker rebuilds indexes and the {@code EmiReloadManager} thread continues
 * (search bake, "Finishing up", {@code Reloaded EMI in ...ms}). Export must wait for
 * {@link EmiReloadManager#isLoaded()}.</p>
 */
public final class EmiExportReadiness {

    private EmiExportReadiness() {}

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
        var manager = EmiApi.getRecipeManager();
        return manager != null && !manager.getRecipes().isEmpty();
    }
}
