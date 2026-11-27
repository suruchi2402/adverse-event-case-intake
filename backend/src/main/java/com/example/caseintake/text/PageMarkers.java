package com.example.caseintake.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page numbers from "Page N of M" markers in the text. A marker opens its page; text before the first marker
 * belongs to that marker's page. Documents without markers have no page numbers.
 */
public final class PageMarkers {

    private static final Pattern MARKER = Pattern.compile("(?i)\\bpage\\s+(\\d+)\\s+of\\s+\\d+\\b");

    private final List<int[]> markers = new ArrayList<>(); // {code point offset, page number}

    public PageMarkers(SourceText source) {
        Matcher m = MARKER.matcher(source.text());
        while (m.find()) {
            markers.add(new int[]{source.codePointOffset(m.start()), Integer.parseInt(m.group(1))});
        }
    }

    public Optional<Integer> pageAt(int codePointOffset) {
        if (markers.isEmpty()) {
            return Optional.empty();
        }
        int page = markers.get(0)[1];
        for (int[] marker : markers) {
            if (marker[0] <= codePointOffset) {
                page = marker[1];
            }
        }
        return Optional.of(page);
    }
}
