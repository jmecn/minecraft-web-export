package io.github.jmecn.minecraftwebexport.emi.lang;
import io.github.jmecn.minecraftwebexport.emi.support.Log;

import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.List;

public final class SearchPinyin {

    private static volatile boolean warnedUnavailable;
    private static volatile HanyuPinyinOutputFormat format;

    private SearchPinyin() {
    }

    private static HanyuPinyinOutputFormat pinyinFormat() {
        HanyuPinyinOutputFormat cached = format;
        if (cached != null) {
            return cached;
        }
        try {
            HanyuPinyinOutputFormat created = new HanyuPinyinOutputFormat();
            created.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            created.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            format = created;
            return created;
        } catch (Throwable t) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                MinecraftWebExportMod.LOGGER.warn(
                        "{} pinyin4j unavailable — Chinese search haystack will omit pinyin tokens: {}",
                        Log.ITEMS_LANG,
                        t.toString());
            }
            return null;
        }
    }

    static boolean containsHan(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    public static List<String> tokensForLabel(String label) {
        try {
            return tokensForLabelInner(label);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static List<String> tokensForLabelInner(String label) throws BadHanyuPinyinOutputFormatCombination {
        if (!containsHan(label)) {
            return List.of();
        }
        HanyuPinyinOutputFormat fmt = pinyinFormat();
        if (fmt == null) {
            return List.of();
        }
        List<String> syllables = new ArrayList<>();
        for (int offset = 0; offset < label.length();) {
            int cp = label.codePointAt(offset);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                String ch = new String(Character.toChars(cp));
                String[] py = PinyinHelper.toHanyuPinyinStringArray(ch.charAt(0), fmt);
                if (py != null && py.length > 0 && !py[0].isBlank()) {
                    syllables.add(py[0]);
                }
            }
            offset += Character.charCount(cp);
        }
        if (syllables.isEmpty()) {
            return List.of();
        }
        String spaced = String.join(" ", syllables);
        String continuous = String.join("", syllables);
        if (continuous.equals(spaced)) {
            return List.of(spaced);
        }
        return List.of(spaced, continuous);
    }
}
