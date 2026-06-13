package io.github.jmecn.minecraftwebexport.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ExportGson {

    public static final Gson GSON = new GsonBuilder().create();

    private ExportGson() {
    }
}
