# Minecraft Web Export

Forge mod that exports EMI recipes as a web bundle.

## Build

```bash
./gradlew jar
# build/libs/minecraft-web-export-<version>.jar
```

| Gradle task         | Purpose                                                      |
|---------------------|--------------------------------------------------------------|
| `runClient`         | Normal client                                                |
| `runExportClient`   | Export in-world (`-DminecraftWebExport.export.enabled=true`) |
| `runExportCiClient` | Short CI-style export smoke test                             |

## Config example

Save as `config/minecraft_web_export-client.toml`. JVM `-DminecraftWebExport.*` overrides matching keys.

```toml
[export]
# CI: auto open world, export once, exit. false = /mwe export only.
# JVM: -DminecraftWebExport.export.enabled=true
enabled = false

# Output root (absolute path). Empty = default under the game directory.
# JVM: -DminecraftWebExport.export.outputDir=/path/to/out
outputDir = ""

# full = all EMI-visible recipes; scoped = seed references only.
# JVM: -DminecraftWebExport.exportMode=full
mode = "full"

# Comma-separated locales. Empty = en_us only.
# JVM: -DminecraftWebExport.exportLanguages=en_us,zh_cn
languages = ""

# Extra namespaces to exclude (comma-separated).
# JVM: -DminecraftWebExport.exportExcludedNamespaces=foo,bar
excludedNamespaces = ""

# Recipe card PNG render scale (1–8). TOML only.
recipeLayoutScale = 2

# true = emi.zip; false = emi/ directory tree.
# JVM: -DminecraftWebExport.export.compressed=false
compressed = true

[ci]
# Ticks on the main menu before auto-creating the export world.
# JVM: -DminecraftWebExport.exportWorldDelayTicks=600
worldDelayTicks = 600

# Whole CI export timeout in seconds. <= 0 disables.
# JVM: -DminecraftWebExport.exportTimeoutSeconds=3600
timeoutSeconds = 3600

# Singleplayer save name for CI export.
# JVM: -DminecraftWebExport.exportWorldName=emi-export
worldName = "emi-export"

[logging]
# Progress log stride; 0 = automatic.
iconLogStride = 0
itemsIndexWriteLogStride = 0
recipeCardLogStride = 0
layoutLogStride = 0

# Log per-item icon failures at WARN instead of DEBUG.
logDetailFailures = false

[icons]
# Unified icon cell size; 0 = per-type sizes below.
iconSize = 0
itemIconSize = 0
blockItemIconSize = 0
fluidIconSize = 0
categoryIconSize = 32
itemIconAtlasMaxSize = 2048
```

No `--quickPlaySingleplayer` with CI export.
