package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.ModEntry;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.PackRef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RoutePackWriter {

    private final Path outputDir;
    private final int packMaxBytes;
    private final Map<String, NamespaceWriter> namespaces = new TreeMap<>();

    public RoutePackWriter(Path outputDir, int packMaxBytes) {
        this.outputDir = outputDir;
        this.packMaxBytes = Math.max(1, packMaxBytes);
    }

    public static int defaultPackMaxBytes() {
        return Math.max(1, Integer.getInteger(Constants.PROP_PACK_MAX_BYTES, Constants.DEFAULT_PACK_MAX_BYTES));
    }

    public void addLayout(String recipeId, JsonObject layout) throws IOException {
        IndexIds.RecipeIdParts parts = IndexIds.splitRecipeId(recipeId);
        if (parts == null) {
            return;
        }
        namespaces
                .computeIfAbsent(parts.namespace(), NamespaceWriter::new)
                .addLayout(parts.path(), layout);
    }

    public BundleMods finish() throws IOException {
        BundleMods.Builder builder = BundleMods.builder();
        for (Map.Entry<String, NamespaceWriter> entry : namespaces.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().finish());
        }
        return builder.build();
    }

    private final class NamespaceWriter {
        private final String namespace;
        private final List<String> routeFiles = new ArrayList<>();
        private final List<PackRef> packRefs = new ArrayList<>();
        private final LinkedHashMap<String, JsonObject> pendingLayouts = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> pendingRoutes = new LinkedHashMap<>();
        private int pendingPackIndex = -1;
        private int routeSequence;
        private int packSequence;

        private NamespaceWriter(String namespace) {
            this.namespace = namespace;
        }

        void addLayout(String path, JsonObject layout) throws IOException {
            if (pendingPackIndex < 0) {
                pendingPackIndex = packRefs.size();
            }
            pendingLayouts.put(path, layout);
            pendingRoutes.put(path, pendingPackIndex);
            if (estimatePackBytes() > packMaxBytes) {
                pendingLayouts.remove(path);
                pendingRoutes.remove(path);
                flushPack();
                pendingPackIndex = packRefs.size();
                pendingLayouts.put(path, layout);
                pendingRoutes.put(path, pendingPackIndex);
            }
            if (estimateRouteShardBytes() > packMaxBytes) {
                flushRouteShard();
            }
        }

        ModEntry finish() throws IOException {
            if (!pendingLayouts.isEmpty()) {
                flushPack();
            }
            if (!pendingRoutes.isEmpty()) {
                flushRouteShard();
            }
            return new ModEntry(List.copyOf(routeFiles), List.copyOf(packRefs));
        }

        private void flushPack() throws IOException {
            if (pendingLayouts.isEmpty()) {
                return;
            }
            int width = BundleDigest.sequenceWidth(packSequence + 1);
            JsonObject root = new JsonObject();
            root.addProperty("schema", Constants.CONTAINER_SCHEMA);
            root.addProperty("namespace", namespace);
            JsonObject layouts = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : pendingLayouts.entrySet()) {
                layouts.add(entry.getKey(), entry.getValue());
            }
            root.add("layouts", layouts);
            byte[] bytes = JsonIO.toUtf8Bytes(root);
            String stem = BundleDigest.stem(packSequence, bytes, width);

            Path packDir = Paths.resolve(outputDir, Constants.RECIPES_LAYOUT_PACKS_DIR + "/" + namespace);
            JsonIO.write(packDir.resolve(stem + ".json"), root);
            packRefs.add(new PackRef(stem, bytes.length));
            packSequence++;
            pendingLayouts.clear();
            pendingPackIndex = -1;
        }

        private void flushRouteShard() throws IOException {
            if (pendingRoutes.isEmpty()) {
                return;
            }
            int width = BundleDigest.sequenceWidth(routeSequence + 1);
            JsonObject root = new JsonObject();
            root.addProperty("schema", Constants.CONTAINER_SCHEMA);
            root.addProperty("namespace", namespace);
            JsonObject routes = new JsonObject();
            for (Map.Entry<String, Integer> entry : pendingRoutes.entrySet()) {
                routes.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("routes", routes);
            byte[] bytes = JsonIO.toUtf8Bytes(root);
            String stem = BundleDigest.stem(routeSequence, bytes, width);

            Path routeDir = Paths.resolve(outputDir, Constants.RECIPES_ROUTES_DIR + "/" + namespace);
            JsonIO.write(routeDir.resolve(stem + ".json"), root);
            routeFiles.add(stem);
            routeSequence++;
            pendingRoutes.clear();
        }

        private int estimatePackBytes() {
            JsonObject root = new JsonObject();
            root.addProperty("schema", Constants.CONTAINER_SCHEMA);
            root.addProperty("namespace", namespace);
            JsonObject layouts = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : pendingLayouts.entrySet()) {
                layouts.add(entry.getKey(), entry.getValue());
            }
            root.add("layouts", layouts);
            return JsonIO.toUtf8Bytes(root).length;
        }

        private int estimateRouteShardBytes() {
            JsonObject root = new JsonObject();
            root.addProperty("schema", Constants.CONTAINER_SCHEMA);
            root.addProperty("namespace", namespace);
            JsonObject routes = new JsonObject();
            for (Map.Entry<String, Integer> entry : pendingRoutes.entrySet()) {
                routes.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("routes", routes);
            return JsonIO.toUtf8Bytes(root).length;
        }
    }
}
