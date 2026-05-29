package io.github.jmecn.minecraftwebexport.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Shared Gson for EMI bundle JSON artifacts (compact, no pretty-printing). */
public final class ExportGson {

    public static final Gson GSON = new GsonBuilder().create();

    private ExportGson() {
    }
}
