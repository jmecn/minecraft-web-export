package io.github.jmecn.minecraftwebexport.config;

import io.github.jmecn.minecraftwebexport.Constants;
import net.minecraftforge.common.ForgeConfigSpec;

@SuppressWarnings("ClassCanBeRecord")
public final class MweClientConfig {

    public final ForgeConfigSpec.BooleanValue exportEnabled;
    public final ForgeConfigSpec.ConfigValue<String> outputDir;
    public final ForgeConfigSpec.ConfigValue<String> exportMode;
    public final ForgeConfigSpec.ConfigValue<String> exportLanguages;
    public final ForgeConfigSpec.ConfigValue<String> excludedNamespaces;
    public final ForgeConfigSpec.BooleanValue exportEmiLayout;
    public final ForgeConfigSpec.IntValue recipeLayoutScale;

    public final ForgeConfigSpec.IntValue worldDelayTicks;
    public final ForgeConfigSpec.IntValue timeoutSeconds;
    public final ForgeConfigSpec.ConfigValue<String> worldName;

    public final ForgeConfigSpec.BooleanValue skipItemIconExport;
    public final ForgeConfigSpec.BooleanValue skipFluidIconExport;
    public final ForgeConfigSpec.BooleanValue skipLangExport;
    public final ForgeConfigSpec.BooleanValue skipCategoryIconExport;
    public final ForgeConfigSpec.BooleanValue skipEmiVisibilityFilter;
    public final ForgeConfigSpec.BooleanValue skipItemNameKeysExport;
    public final ForgeConfigSpec.BooleanValue skipTagMembersIndexExport;
    public final ForgeConfigSpec.BooleanValue skipItemsSearchExport;
    public final ForgeConfigSpec.BooleanValue skipEmiLayoutExport;

    public final ForgeConfigSpec.IntValue iconLogStride;
    public final ForgeConfigSpec.IntValue itemsIndexWriteLogStride;
    public final ForgeConfigSpec.IntValue recipeCardLogStride;
    public final ForgeConfigSpec.IntValue layoutLogStride;
    public final ForgeConfigSpec.BooleanValue logDetailFailures;

    public final ForgeConfigSpec.IntValue iconSize;
    public final ForgeConfigSpec.IntValue itemIconSize;
    public final ForgeConfigSpec.IntValue blockItemIconSize;
    public final ForgeConfigSpec.IntValue fluidIconSize;
    public final ForgeConfigSpec.IntValue categoryIconSize;
    public final ForgeConfigSpec.IntValue itemIconAtlasMaxSize;

    MweClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("export");
        exportEnabled = builder
                .comment("CI mode: auto open world, export once, then exit. false = command-only export.")
                .define("enabled", false);
        outputDir = builder
                .comment("Optional absolute output root. Empty uses the default under the game directory.")
                .define("outputDir", "");
        exportMode = builder
                .comment("Export scope: full (all EMI-visible recipes) or scoped (seed closure only).")
                .define("mode", "full");
        exportLanguages = builder
                .comment("Comma-separated language codes to export. Empty exports en_us only.")
                .define("languages", "");
        excludedNamespaces = builder
                .comment("Extra comma-separated namespaces excluded from export.")
                .define("excludedNamespaces", "");
        exportEmiLayout = builder
                .comment("Write EMI recipe layout JSON during export.")
                .define("exportEmiLayout", true);
        recipeLayoutScale = builder
                .comment("Recipe layout render scale.")
                .defineInRange("recipeLayoutScale", Constants.DEFAULT_RECIPE_LAYOUT_SCALE, 1, 8);
        builder.pop();

        builder.push("ci");
        worldDelayTicks = builder
                .comment("Ticks to wait on the idle main menu before auto-creating the export world.")
                .defineInRange("worldDelayTicks", Constants.CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS, 0, 20_000);
        timeoutSeconds = builder
                .comment("Hard timeout for the whole CI export flow. Values <= 0 disable the timeout.")
                .defineInRange("timeoutSeconds", Constants.CI_DEFAULT_EXPORT_TIMEOUT_SECONDS, 0, 86_400);
        worldName = builder
                .comment("Singleplayer save name used by CI export.")
                .define("worldName", Constants.CI_DEFAULT_EXPORT_WORLD_NAME);
        builder.pop();

        builder.push("features");
        skipItemIconExport = builder.define("skipItemIconExport", false);
        skipFluidIconExport = builder.define("skipFluidIconExport", false);
        skipLangExport = builder.define("skipLangExport", false);
        skipCategoryIconExport = builder.define("skipCategoryIconExport", false);
        skipEmiVisibilityFilter = builder.define("skipEmiVisibilityFilter", false);
        skipItemNameKeysExport = builder.define("skipItemNameKeysExport", false);
        skipTagMembersIndexExport = builder.define("skipTagMembersIndexExport", false);
        skipItemsSearchExport = builder.define("skipItemsSearchExport", false);
        skipEmiLayoutExport = builder.define("skipEmiLayoutExport", false);
        builder.pop();

        builder.push("logging");
        iconLogStride = builder
                .comment("Progress log stride for icon export. 0 selects an automatic stride.")
                .defineInRange("iconLogStride", 0, 0, 1_000_000);
        itemsIndexWriteLogStride = builder
                .comment("Progress log stride for item index writes. 0 selects an automatic stride.")
                .defineInRange("itemsIndexWriteLogStride", 0, 0, 1_000_000);
        recipeCardLogStride = builder
                .comment("Progress log stride for recipe cards. 0 selects an automatic stride.")
                .defineInRange("recipeCardLogStride", 0, 0, 1_000_000);
        layoutLogStride = builder
                .comment("Progress log stride for layout building. 0 selects an automatic stride.")
                .defineInRange("layoutLogStride", 0, 0, 1_000_000);
        logDetailFailures = builder
                .comment("Log per-item icon detail failures at WARN instead of DEBUG.")
                .define("logDetailFailures", false);
        builder.pop();

        builder.push("icons");
        iconSize = builder
                .comment("Unified icon cell size. 0 falls back to per-type sizes or defaults.")
                .defineInRange("iconSize", 0, 0, 256);
        itemIconSize = builder
                .comment("Item icon cell size when iconSize is 0. 0 means unset.")
                .defineInRange("itemIconSize", 0, 0, 256);
        blockItemIconSize = builder
                .comment("Block-item icon cell size when iconSize is 0. 0 means unset.")
                .defineInRange("blockItemIconSize", 0, 0, 256);
        fluidIconSize = builder
                .comment("Fluid icon cell size when iconSize is 0. 0 means unset.")
                .defineInRange("fluidIconSize", 0, 0, 256);
        categoryIconSize = builder
                .comment("Category icon cell size.")
                .defineInRange("categoryIconSize", Constants.DEFAULT_CATEGORY_ICON_SIZE, 8, 256);
        itemIconAtlasMaxSize = builder
                .comment("Maximum item icon atlas dimension.")
                .defineInRange("itemIconAtlasMaxSize", Constants.DEFAULT_ATLAS_MAX_SIZE, 256, 8192);
        builder.pop();
    }
}
