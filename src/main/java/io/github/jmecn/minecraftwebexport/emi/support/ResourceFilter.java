package io.github.jmecn.minecraftwebexport.emi.support;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class ResourceFilter {

    private ResourceFilter() {}

    public static Set<String> excludedNamespaces() {
        return mergeExcludedNamespaces(MweConfig.excludedNamespaces());
    }

    static Set<String> mergeExcludedNamespaces(String extra) {
        String trimmed = extra == null ? "" : extra.trim();
        if (trimmed.isEmpty()) {
            return Constants.DEFAULT_EXCLUDED_NAMESPACES;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(Constants.DEFAULT_EXCLUDED_NAMESPACES);
        Arrays.stream(trimmed.split(","))
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
