package io.github.jmecn.minecraftwebexport.export.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for {@link ExportModule} implementations supplied by companion mods.
 *
 * <p>Register during mod setup, e.g. {@code ExportModuleRegistry.register(new MyExportModule());}</p>
 */
public final class ExportModuleRegistry {

    private static final CopyOnWriteArrayList<ExportModule> MODULES = new CopyOnWriteArrayList<>();

    private ExportModuleRegistry() {
    }

    public static void register(ExportModule module) {
        Objects.requireNonNull(module, "module");
        String id = module.moduleId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ExportModule.moduleId() must be non-empty");
        }
        for (ExportModule existing : MODULES) {
            if (id.equals(existing.moduleId())) {
                throw new IllegalStateException("ExportModule already registered: " + id);
            }
        }
        MODULES.add(module);
    }

    public static List<ExportModule> modules() {
        return Collections.unmodifiableList(new ArrayList<>(MODULES));
    }

    static void clearForTests() {
        MODULES.clear();
    }
}
