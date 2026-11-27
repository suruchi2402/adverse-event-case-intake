package com.example.caseintake.text;

import java.util.ArrayList;
import java.util.List;

/**
 * A source document's text, addressed in Unicode code points as the output schema requires.
 * Also holds a folded copy (lower case, whitespace runs collapsed, typographic dashes and quotes made plain)
 * with a map back to code point offsets, which {@link QuoteLocator} searches.
 */
public final class SourceText {

    private final String text;
    private final int[] codePoints;
    private final String folded;
    private final int[] foldedToOriginal;

    public SourceText(String text) {
        this.text = text;
        this.codePoints = text.codePoints().toArray();
        StringBuilder sb = new StringBuilder();
        List<Integer> map = new ArrayList<>();
        boolean inWhitespace = false;
        for (int i = 0; i < codePoints.length; i++) {
            int cp = codePoints[i];
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                if (!inWhitespace && !sb.isEmpty()) {
                    sb.append(' ');
                    map.add(i);
                }
                inWhitespace = true;
                continue;
            }
            inWhitespace = false;
            for (char c : Character.toChars(foldCodePoint(cp))) {
                sb.append(c);
                map.add(i);
            }
        }
        this.folded = sb.toString();
        this.foldedToOriginal = map.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Lower case; en and em dashes become '-', curly quotes become straight ones. Always one code point out. */
    static int foldCodePoint(int cp) {
        return switch (cp) {
            case '–', '—', '−' -> '-';
            case '‘', '’' -> '\'';
            case '“', '”' -> '"';
            default -> Character.toLowerCase(cp);
        };
    }

    /** The same folding applied to a quote: trimmed, whitespace runs collapsed. */
    public static String fold(String s) {
        StringBuilder sb = new StringBuilder();
        boolean inWhitespace = false;
        for (int cp : s.codePoints().toArray()) {
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                inWhitespace = true;
                continue;
            }
            if (inWhitespace && !sb.isEmpty()) {
                sb.append(' ');
            }
            inWhitespace = false;
            sb.appendCodePoint(foldCodePoint(cp));
        }
        return sb.toString();
    }

    public String text() {
        return text;
    }

    public int length() {
        return codePoints.length;
    }

    public String slice(int start, int end) {
        return new String(codePoints, start, end - start);
    }

    String folded() {
        return folded;
    }

    int originalOffset(int foldedIndex) {
        return foldedToOriginal[foldedIndex];
    }

    /** Converts a UTF-16 index from a {@link java.util.regex.Matcher} into a code point offset. */
    public int codePointOffset(int utf16Index) {
        return text.codePointCount(0, utf16Index);
    }
}
