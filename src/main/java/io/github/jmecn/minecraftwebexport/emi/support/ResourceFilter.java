package io.github.jmecn.minecraftwebexport.emi.support;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ResourceFilter {

    private static final Set<String> DEFAULT_EXCLUDED = Set.of("additionalplacements");

    private ResourceFilter() {
    }

    public static Set<String> excludedNamespaces() {
        String extra = System.getProperty("minecraftWebExport.exportExcludedNamespaces", "").trim();
        if (extra.isEmpty()) {
            return DEFAULT_EXCLUDED;
        }
        var merged = new LinkedHashSet<>(DEFAULT_EXCLUDED);
        Arrays.stream(extra.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(merged::add);
        return Set.copyOf(merged);
    }

    public static boolean isExcluded(ResourceLocation id) {
        return excludedNamespaces().contains(id.getNamespace());
    }
}
