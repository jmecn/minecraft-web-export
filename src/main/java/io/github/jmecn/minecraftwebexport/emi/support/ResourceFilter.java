package io.github.jmecn.minecraftwebexport.emi.support;

import io.github.jmecn.minecraftwebexport.Constants;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ResourceFilter {

    private ResourceFilter() {}

    public static Set<String> excludedNamespaces() {
        String extra = System.getProperty(Constants.PROP_EXPORT_EXCLUDED_NAMESPACES, "").trim();
        if (extra.isEmpty()) {
            return Constants.DEFAULT_EXCLUDED_NAMESPACES;
        }
        var merged = new LinkedHashSet<>(Constants.DEFAULT_EXCLUDED_NAMESPACES);
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
