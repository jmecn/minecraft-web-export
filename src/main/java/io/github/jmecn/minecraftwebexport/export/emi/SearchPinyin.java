package io.github.jmecn.minecraftwebexport.export.emi;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.List;

/** Pinyin tokens for Chinese item search haystacks (aligned with emi-bundle-optimize pinyin-pro). */
public final class SearchPinyin {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
    }

    private SearchPinyin() {
    }

    static boolean containsHan(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    static List<String> tokensForLabel(String label) {
        if (!containsHan(label)) {
            return List.of();
        }
        List<String> syllables = new ArrayList<>();
        for (int offset = 0; offset < label.length();) {
            int cp = label.codePointAt(offset);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                String ch = new String(Character.toChars(cp));
                try {
                    String[] py = PinyinHelper.toHanyuPinyinStringArray(ch.charAt(0), FORMAT);
                    if (py != null && py.length > 0 && !py[0].isBlank()) {
                        syllables.add(py[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    // skip character
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
