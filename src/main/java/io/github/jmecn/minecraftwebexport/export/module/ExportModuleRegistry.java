package io.github.jmecn.minecraftwebexport.export.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

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
