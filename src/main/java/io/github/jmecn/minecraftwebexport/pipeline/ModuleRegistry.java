package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.pipeline.Module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModuleRegistry {

    private static final CopyOnWriteArrayList<Module> MODULES = new CopyOnWriteArrayList<>();

    private ModuleRegistry() {
    }

    public static void register(Module module) {
        Objects.requireNonNull(module, "module");
        String id = module.moduleId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Module.moduleId() must be non-empty");
        }
        for (Module existing : MODULES) {
            if (id.equals(existing.moduleId())) {
                throw new IllegalStateException("Module already registered: " + id);
            }
        }
        MODULES.add(module);
    }

    public static List<Module> modules() {
        return Collections.unmodifiableList(new ArrayList<>(MODULES));
    }

    static void clearForTests() {
        MODULES.clear();
    }
}
