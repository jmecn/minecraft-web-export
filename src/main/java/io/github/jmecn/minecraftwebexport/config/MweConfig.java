package io.github.jmecn.minecraftwebexport.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.jmecn.minecraftwebexport.Constants;
import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public final class MweConfig {

    public static MweClientConfig CLIENT;
    static ForgeConfigSpec CLIENT_SPEC;
    private static volatile String runtimeExportModeOverride;

    private MweConfig() {}

    @SuppressWarnings("removal")
    public static void register() {
        Pair<MweClientConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(MweClientConfig::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static void ensureForTests() {
        if (CLIENT == null) {
            Pair<MweClientConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(MweClientConfig::new);
            CLIENT = pair.getLeft();
            CLIENT_SPEC = pair.getRight();
            ConfigParser<CommentedConfig> parser = TomlFormat.instance().createParser();
            CommentedConfig defaults = parser.parse("""
                    [export]
                    enabled = false
                    outputDir = ""
                    mode = "full"
                    languages = ""
                    excludedNamespaces = ""
                    exportEmiLayout = true
                    recipeLayoutScale = %d

                    [ci]
                    worldDelayTicks = %d
                    timeoutSeconds = %d
                    worldName = "%s"

                    [features]

                    [logging]

                    [icons]
                    categoryIconSize = %d
                    itemIconAtlasMaxSize = %d
                    """.formatted(
                    Constants.DEFAULT_RECIPE_LAYOUT_SCALE,
                    Constants.CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS,
                    Constants.CI_DEFAULT_EXPORT_TIMEOUT_SECONDS,
                    Constants.CI_DEFAULT_EXPORT_WORLD_NAME,
                    Constants.DEFAULT_CATEGORY_ICON_SIZE,
                    Constants.DEFAULT_ATLAS_MAX_SIZE));
            CLIENT_SPEC.acceptConfig(defaults);
        }
    }

    public static void clearForTests() {
        CLIENT = null;
        CLIENT_SPEC = null;
        runtimeExportModeOverride = null;
        MweClientBootstrap.clearForTests();
    }

    private static boolean configReady() {
        return CLIENT_SPEC != null && CLIENT_SPEC.isLoaded();
    }

    public static void setRuntimeExportModeOverride(String mode) {
        runtimeExportModeOverride = mode;
    }

    public static void clearRuntimeExportModeOverride() {
        runtimeExportModeOverride = null;
    }

    private static MweClientConfig client() {
        if (CLIENT == null) {
            ensureForTests();
        }
        return CLIENT;
    }

    public static String exportOutputDir() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_OUTPUT_DIR)) {
            return MweJvmCompat.getString(MweJvmCompat.EXPORT_OUTPUT_DIR);
        }
        if (!configReady()) {
            return "";
        }
        return client().outputDir.get().trim();
    }

    public static String exportMode() {
        if (runtimeExportModeOverride != null) {
            return runtimeExportModeOverride.trim();
        }
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_MODE)) {
            return MweJvmCompat.getString(MweJvmCompat.EXPORT_MODE);
        }
        if (!configReady()) {
            return "full";
        }
        return client().exportMode.get().trim();
    }

    public static String exportLanguages() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_LANGUAGES)) {
            return MweJvmCompat.getString(MweJvmCompat.EXPORT_LANGUAGES);
        }
        return client().exportLanguages.get().trim();
    }

    public static String excludedNamespaces() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_EXCLUDED_NAMESPACES)) {
            return MweJvmCompat.getString(MweJvmCompat.EXPORT_EXCLUDED_NAMESPACES);
        }
        return client().excludedNamespaces.get().trim();
    }

    public static boolean exportEnabled() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_ENABLED)) {
            return MweJvmCompat.getBoolean(MweJvmCompat.EXPORT_ENABLED);
        }
        if (!configReady()) {
            return false;
        }
        return client().exportEnabled.get();
    }

    public static boolean exportEmiLayout() {
        return client().exportEmiLayout.get();
    }

    public static int recipeLayoutScale() {
        return Math.max(1, client().recipeLayoutScale.get());
    }

    public static int worldDelayTicks() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_WORLD_DELAY_TICKS)) {
            return Math.max(0, MweJvmCompat.getInt(MweJvmCompat.EXPORT_WORLD_DELAY_TICKS, Constants.CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS));
        }
        return Math.max(0, client().worldDelayTicks.get());
    }

    public static int timeoutSeconds() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_TIMEOUT_SECONDS)) {
            return MweJvmCompat.getInt(MweJvmCompat.EXPORT_TIMEOUT_SECONDS, Constants.CI_DEFAULT_EXPORT_TIMEOUT_SECONDS);
        }
        return client().timeoutSeconds.get();
    }

    public static String exportWorldName() {
        if (MweJvmCompat.has(MweJvmCompat.EXPORT_WORLD_NAME)) {
            String raw = MweJvmCompat.getString(MweJvmCompat.EXPORT_WORLD_NAME);
            return raw.isEmpty() ? Constants.CI_DEFAULT_EXPORT_WORLD_NAME : raw;
        }
        String raw = client().worldName.get().trim();
        return raw.isEmpty() ? Constants.CI_DEFAULT_EXPORT_WORLD_NAME : raw;
    }

    public static boolean skipItemIconExport() {
        return client().skipItemIconExport.get();
    }

    public static boolean skipFluidIconExport() {
        return client().skipFluidIconExport.get();
    }

    public static boolean skipLangExport() {
        return client().skipLangExport.get();
    }

    public static boolean skipCategoryIconExport() {
        return client().skipCategoryIconExport.get();
    }

    public static boolean skipEmiVisibilityFilter() {
        return client().skipEmiVisibilityFilter.get();
    }

    public static boolean skipItemNameKeysExport() {
        return client().skipItemNameKeysExport.get();
    }

    public static boolean skipTagMembersIndexExport() {
        return client().skipTagMembersIndexExport.get();
    }

    public static boolean skipItemsSearchExport() {
        return client().skipItemsSearchExport.get();
    }

    public static boolean skipEmiLayoutExport() {
        return client().skipEmiLayoutExport.get();
    }

    public static int iconLogStride() {
        return client().iconLogStride.get();
    }

    public static int itemsIndexWriteLogStride() {
        return client().itemsIndexWriteLogStride.get();
    }

    public static int recipeCardLogStride() {
        return client().recipeCardLogStride.get();
    }

    public static int layoutLogStride() {
        return client().layoutLogStride.get();
    }

    public static boolean logDetailFailures() {
        return client().logDetailFailures.get();
    }

    public static int iconSize() {
        return client().iconSize.get();
    }

    public static int itemIconSize() {
        return client().itemIconSize.get();
    }

    public static int blockItemIconSize() {
        return client().blockItemIconSize.get();
    }

    public static int fluidIconSize() {
        return client().fluidIconSize.get();
    }

    public static int categoryIconSize() {
        return client().categoryIconSize.get();
    }

    public static int itemIconAtlasMaxSize() {
        return client().itemIconAtlasMaxSize.get();
    }

    public static boolean timedOut(long startNanos) {
        int sec = timeoutSeconds();
        if (sec <= 0) {
            return false;
        }
        return (System.nanoTime() - startNanos) >= sec * 1_000_000_000L;
    }

    public static String parseExportMode(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "scoped", "closure" -> "scoped";
            default -> "full";
        };
    }
}
