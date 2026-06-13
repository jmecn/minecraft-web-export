# Minecraft Web Export

Forge mod that exports EMI recipe bundles for web viewers ([emi-recipe-renderer](https://www.npmjs.com/package/emi-recipe-renderer), [emi-bundle-optimize](https://www.npmjs.com/package/emi-bundle-optimize)).

## Build

```bash
./gradlew jar
# build/libs/minecraft-web-export-<version>.jar
```

## GitHub Release and Packages

Each `v*` tag triggers [.github/workflows/release.yml](.github/workflows/release.yml) to:

1. **Publish** to GitHub Packages Maven: `io.github.jmecn:minecraft-web-export:<version>` (same mod jar as below).
2. **Upload** `build/libs/minecraft-web-export-<version>.jar` to GitHub Releases (used by modpack CI such as TFG-Recipe-Viewer).

| Channel | Use |
|---------|-----|
| **GitHub Releases** (jar) | Runtime: put the jar in `mods/` |
| **GitHub Packages** (Maven) | Compile-time: depend from Gradle in companion mods (e.g. `field-guide-export`) |

Repository URL: `https://maven.pkg.github.com/jmecn/minecraft-web-export`

### Consuming from GitHub Packages (Gradle)

In `~/.gradle/gradle.properties` (local) or CI secrets:

```properties
gpr.user=<GitHub username>
gpr.key=<PAT with read:packages>
```

Or set `GITHUB_ACTOR` and `GITHUB_TOKEN` in the environment.

```gradle
repositories {
    mavenCentral()
    maven {
        name = 'GitHubPackagesMinecraftWebExport'
        url = uri('https://maven.pkg.github.com/jmecn/minecraft-web-export')
        credentials {
            username = findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    def coreVersion = '0.3.1'
    modCompileOnly fg.deobf("io.github.jmecn:minecraft-web-export:${coreVersion}")
    modRuntimeOnly fg.deobf("io.github.jmecn:minecraft-web-export:${coreVersion}")
}
```

LegacyForge / MDG: use `fg.deobf(...)` on the dependency. Match `coreVersion` to a tag that has been published (see [Packages](https://github.com/jmecn/minecraft-web-export/packages)).

Local publish (maintainers):

```bash
export GITHUB_ACTOR=your-user
export GITHUB_TOKEN=ghp_...   # write:packages
./gradlew publish
```

## Gradle runs

| Task | Purpose |
|------|---------|
| `runClient` | Normal client |
| `runExportClient` | Export in-world (`-DminecraftWebExport.export.enabled=true`) |
| `runExportCiClient` | Short CI-style export smoke |

## Export output

Writes an EMI bundle under `<outputDir>/emi/`:

- `bundle.json` — metadata + per-mod route/pack manifests
- `recipes/routes/`, `recipes/layout-packs/` — recipe layouts
- `categories/index.json` — EMI recipe categories (tabs)
- `items/index.json`, `items/<namespace>/*.json` — item reverse index (`schema: 2`, grouped by category)
- `icons/`, `textures/`, `lang/`, `tags/` — assets and indexes

## Headless / CI JVM properties

| Property | Example | Description |
|----------|---------|-------------|
| `minecraftWebExport.export.enabled` | `true` | CI mode: auto open world, export, exit. `false` = command-only |
| `minecraftWebExport.export.outputDir` | absolute path | Output root |
| `minecraftWebExport.exportWorldDelayTicks` | `600` | Menu delay before new world |
| `minecraftWebExport.exportTimeoutSeconds` | `3600` | Hard timeout (`<=0` off) |

Large modpacks: use **≥12G** heap. Do not pass `--quickPlaySingleplayer` with CI export enabled.
