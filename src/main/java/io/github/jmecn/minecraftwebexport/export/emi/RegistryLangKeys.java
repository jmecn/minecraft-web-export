package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.ArrayList;
import java.util.List;

/** Minecraft lang keys for registry ids (aligned with emi-recipe-renderer lookup order). */
public final class RegistryLangKeys {

    private RegistryLangKeys() {
    }

    /** Mod namespace from a registry id, e.g. {@code gtceu:ingot} → {@code gtceu}. */
    public static String namespace(String registryId) {
        String bare = normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        return colon > 0 ? bare.substring(0, colon) : "";
    }

    /**
     * GregTech CEu items use {@code material.*} + {@code tagprefix.*} in lang files.
     * Filling {@code item.gtceu.*} from hover names pollutes non-English locales with English.
     */
    public static boolean skipRegistryLangFill(String registryId) {
        return "gtceu".equals(namespace(registryId));
    }

    public static String normalizeRegistryId(String registryId) {
        if (registryId == null) {
            return "";
        }
        String id = registryId.trim();
        if (id.startsWith("item:")) {
            id = id.substring(5);
        }
        int brace = id.indexOf('{');
        if (brace >= 0) {
            id = id.substring(0, brace);
        }
        int at = id.indexOf('@');
        if (at >= 0) {
            id = id.substring(0, at);
        }
        return id;
    }

    public static String dottedRegistryId(String registryId) {
        String bare = normalizeRegistryId(registryId);
        return bare.replace('/', '.').replace(':', '.');
    }

    public static String itemKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "item." + dotted;
    }

    public static String blockKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "block." + dotted;
    }

    public static String fluidKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "fluid." + dotted;
    }

    /** Keys tried by the web renderer for items (item first, then block/fluid fallbacks). */
    public static List<String> itemLookupKeys(String registryId) {
        String dotted = dottedRegistryId(registryId);
        if (dotted.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(3);
        keys.add("item." + dotted);
        keys.add("block." + dotted);
        keys.add("fluid." + dotted);
        return keys;
    }

    public static List<String> fluidLookupKeys(String registryId) {
        String dotted = dottedRegistryId(registryId);
        if (dotted.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(3);
        keys.add("fluid." + dotted);
        keys.add("item." + dotted);
        keys.add("block." + dotted);
        return keys;
    }
}
