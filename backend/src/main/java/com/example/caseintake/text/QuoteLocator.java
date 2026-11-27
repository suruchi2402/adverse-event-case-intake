package com.example.caseintake.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds a model's quote in the source text. Case, runs of whitespace and typographic dashes or quotes are ignored,
 * as the checker ignores them. A quote the model shortened with an ellipsis is located part by part.
 * The model never supplies offsets: models are unreliable at counting characters, so the pipeline computes them.
 */
public final class QuoteLocator {

    /** The checker rejects quotes shorter than this (after folding). */
    public static final int MIN_QUOTE_LENGTH = 3;

    private static final Pattern ELLIPSIS = Pattern.compile("\\s*(?:\\[\\s*(?:\\.\\.\\.|…)\\s*]|\\.\\.\\.|…)\\s*");
    private static final String SURROUNDING_MARKS = "\"'`“”‘’";
    private static final String TRAILING_PUNCTUATION = ".,;:";

    private QuoteLocator() {
    }

    /** A located span in code points, start inclusive, end exclusive. */
    public record Span(int start, int end) {
    }

    /**
     * Every part of the quote, located in order of appearance in the quote, or empty when any part is missing.
     * A part that occurs more than once is matched at its first occurrence after the previous part.
     */
    public static Optional<List<Span>> locate(SourceText source, String quote) {
        if (quote == null) {
            return Optional.empty();
        }
        List<Span> spans = new ArrayList<>();
        int cursor = 0;
        for (String part : ELLIPSIS.split(stripSurroundingMarks(quote.strip()))) {
            if (SourceText.fold(part).length() < MIN_QUOTE_LENGTH) {
                continue;
            }
            Optional<Span> span = locatePart(source, part, cursor).or(() -> locatePart(source, part, 0));
            if (span.isEmpty()) {
                return Optional.empty();
            }
            spans.add(span.get());
            cursor = span.get().end();
        }
        return spans.isEmpty() ? Optional.empty() : Optional.of(spans);
    }

    private static Optional<Span> locatePart(SourceText source, String part, int fromCodePoint) {
        Optional<Span> exact = find(source, SourceText.fold(part), fromCodePoint);
        if (exact.isPresent()) {
            return exact;
        }
        String trimmed = stripTrailingPunctuation(part);
        return trimmed.equals(part) ? Optional.empty() : find(source, SourceText.fold(trimmed), fromCodePoint);
    }

    private static Optional<Span> find(SourceText source, String foldedQuote, int fromCodePoint) {
        if (foldedQuote.length() < MIN_QUOTE_LENGTH) {
            return Optional.empty();
        }
        String haystack = source.folded();
        int index = haystack.indexOf(foldedQuote);
        while (index >= 0 && source.originalOffset(index) < fromCodePoint) {
            index = haystack.indexOf(foldedQuote, index + 1);
        }
        if (index < 0) {
            return Optional.empty();
        }
        int start = source.originalOffset(index);
        int end = source.originalOffset(index + foldedQuote.length() - 1) + 1;
        return Optional.of(new Span(start, end));
    }

    private static String stripSurroundingMarks(String s) {
        int from = 0;
        int to = s.length();
        while (from < to && SURROUNDING_MARKS.indexOf(s.charAt(from)) >= 0) {
            from++;
        }
        while (to > from && SURROUNDING_MARKS.indexOf(s.charAt(to - 1)) >= 0) {
            to--;
        }
        return s.substring(from, to);
    }

    private static String stripTrailingPunctuation(String s) {
        int to = s.length();
        while (to > 0 && TRAILING_PUNCTUATION.indexOf(s.charAt(to - 1)) >= 0) {
            to--;
        }
        return s.substring(0, to).strip();
    }
}
