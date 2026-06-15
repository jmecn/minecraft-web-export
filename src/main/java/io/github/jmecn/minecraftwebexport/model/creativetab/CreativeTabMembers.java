package io.github.jmecn.minecraftwebexport.model.creativetab;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;

public record CreativeTabMembers(int schema, String tab, int itemCount, List<String> items) {

    public CreativeTabMembers {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public static CreativeTabMembers of(String tab, List<String> items) {
        return new CreativeTabMembers(
                Constants.CREATIVE_TAB_MEMBERS_SCHEMA,
                tab,
                items == null ? 0 : items.size(),
                items);
    }
}
