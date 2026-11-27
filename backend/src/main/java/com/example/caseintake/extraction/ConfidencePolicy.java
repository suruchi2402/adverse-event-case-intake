package com.example.caseintake.extraction;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.text.DateMentions;
import com.example.caseintake.text.DateMentions.DateMention;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Caps on the model's confidence that the pipeline can check for itself, by field type only.
 * A cap never changes a value; it only lowers the penalty if the value is wrong, so it can only help the score.
 * <ul>
 *   <li>date: high only when a quote writes out that exact date and the reading is not day/month ambiguous;</li>
 *   <li>integer and number: high only when a quote contains the number itself (not, say, a year of birth).</li>
 * </ul>
 */
public final class ConfidencePolicy {

    public record Cap(Confidence max, String reason) {
    }

    public Optional<Cap> capFor(FieldDefinition field, Object value, List<String> quotedTexts) {
        return switch (field.type()) {
            case DATE -> dateCap(value, quotedTexts);
            case INTEGER, NUMBER -> numberCap(value, quotedTexts);
            default -> Optional.empty();
        };
    }

    private static Optional<Cap> dateCap(Object value, List<String> quotedTexts) {
        LocalDate date = LocalDate.parse((String) value);
        List<DateMention> mentions = quotedTexts.stream().flatMap(q -> DateMentions.find(q).stream()).toList();
        boolean stated = mentions.stream().anyMatch(m -> m.date().equals(date) && !m.ambiguous());
        if (stated) {
            return Optional.empty();
        }
        boolean ambiguous = mentions.stream().anyMatch(m -> m.date().equals(date));
        return Optional.of(new Cap(Confidence.MEDIUM, ambiguous
                ? "The quoted date can be read day-first or month-first; confidence capped at medium."
                : "The date is not written out in the quotes (derived or relative); confidence capped at medium."));
    }

    private static Optional<Cap> numberCap(Object value, List<String> quotedTexts) {
        String plain = new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        Pattern asToken = Pattern.compile("(?<![\\d.])" + Pattern.quote(plain) + "(?![\\d]|\\.\\d)");
        boolean stated = quotedTexts.stream().anyMatch(q -> asToken.matcher(q).find());
        return stated ? Optional.empty()
                : Optional.of(new Cap(Confidence.MEDIUM, "The number " + plain + " is not written in the quotes (derived); confidence capped at medium."));
    }
}
