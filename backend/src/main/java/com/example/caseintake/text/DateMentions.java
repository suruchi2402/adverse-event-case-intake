package com.example.caseintake.text;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dates written out in a piece of text, in the common English and numeric forms.
 * Used to tell a date the document states from one the model computed, and to spot day/month ambiguity
 * such as 03/04/2026, which is 3 April in most of Europe and 4 March in the United States.
 */
public final class DateMentions {

    /** A date the text can be read as. Ambiguous when the same digits could also be read with day and month swapped. */
    public record DateMention(LocalDate date, boolean ambiguous) {
    }

    private static final String MONTH = "(january|february|march|april|may|june|july|august|september|october|november|december"
            + "|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\.?";
    private static final String ORDINAL = "(?:st|nd|rd|th)?";
    private static final Pattern ISO = Pattern.compile("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b");
    private static final Pattern NUMERIC = Pattern.compile("\\b(\\d{1,2})[./](\\d{1,2})[./](\\d{4})\\b");
    private static final Pattern DAY_MONTH_YEAR = Pattern.compile("(?i)\\b(\\d{1,2})" + ORDINAL + "\\s+(?:of\\s+)?" + MONTH + ",?\\s+(\\d{4})\\b");
    private static final Pattern MONTH_DAY_YEAR = Pattern.compile("(?i)\\b" + MONTH + "\\s+(\\d{1,2})" + ORDINAL + ",?\\s+(\\d{4})\\b");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mar", 3), Map.entry("apr", 4), Map.entry("may", 5),
            Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("aug", 8), Map.entry("sep", 9), Map.entry("oct", 10),
            Map.entry("nov", 11), Map.entry("dec", 12));

    private DateMentions() {
    }

    public static List<DateMention> find(String text) {
        List<DateMention> found = new ArrayList<>();
        Matcher iso = ISO.matcher(text);
        while (iso.find()) {
            date(iso.group(1), iso.group(2), iso.group(3)).ifPresent(d -> found.add(new DateMention(d, false)));
        }
        Matcher numeric = NUMERIC.matcher(text);
        while (numeric.find()) {
            Optional<LocalDate> dayFirst = date(numeric.group(3), numeric.group(2), numeric.group(1));
            Optional<LocalDate> monthFirst = date(numeric.group(3), numeric.group(1), numeric.group(2));
            boolean ambiguous = dayFirst.isPresent() && monthFirst.isPresent() && !dayFirst.equals(monthFirst);
            dayFirst.ifPresent(d -> found.add(new DateMention(d, ambiguous)));
            if (ambiguous) {
                monthFirst.ifPresent(d -> found.add(new DateMention(d, true)));
            } else if (dayFirst.isEmpty()) {
                monthFirst.ifPresent(d -> found.add(new DateMention(d, false)));
            }
        }
        Matcher dmy = DAY_MONTH_YEAR.matcher(text);
        while (dmy.find()) {
            named(dmy.group(3), dmy.group(2), dmy.group(1)).ifPresent(d -> found.add(new DateMention(d, false)));
        }
        Matcher mdy = MONTH_DAY_YEAR.matcher(text);
        while (mdy.find()) {
            named(mdy.group(3), mdy.group(1), mdy.group(2)).ifPresent(d -> found.add(new DateMention(d, false)));
        }
        return found;
    }

    private static Optional<LocalDate> named(String year, String monthName, String day) {
        Integer month = MONTHS.get(monthName.toLowerCase(Locale.ROOT).substring(0, 3));
        return month == null ? Optional.empty() : date(year, String.valueOf(month), day);
    }

    private static Optional<LocalDate> date(String year, String month, String day) {
        try {
            return Optional.of(LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day)));
        } catch (DateTimeException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
