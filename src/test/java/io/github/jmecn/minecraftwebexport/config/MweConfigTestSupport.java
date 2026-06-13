package io.github.jmecn.minecraftwebexport.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.util.Map;

public final class MweConfigTestSupport {

    private MweConfigTestSupport() {}

    public static void apply(Map<String, Object> values) {
        MweConfig.ensureForTests();
        StringBuilder toml = new StringBuilder();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            toml.append(entry.getKey()).append(" = ");
            Object value = entry.getValue();
            if (value instanceof String string) {
                toml.append('"').append(string).append('"');
            } else {
                toml.append(value);
            }
            toml.append('\n');
        }
        ConfigParser<CommentedConfig> parser = TomlFormat.instance().createParser();
        CommentedConfig config = parser.parse(toml.toString());
        MweConfig.CLIENT_SPEC.acceptConfig(config);
    }
}
