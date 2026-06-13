package io.github.jmecn.minecraftwebexport.model.bundle;

import java.util.List;

public record ItemsLangRef(String dir, List<String> locales) {

    public ItemsLangRef {
        locales = List.copyOf(locales == null ? List.of() : locales);
    }
}
