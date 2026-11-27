package com.example.caseintake.extraction;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.extraction.ModelOutput.ModelField;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.result.FieldResult;
import com.example.caseintake.result.FieldStatus;
import com.example.caseintake.result.Provenance;
import com.example.caseintake.text.PageMarkers;
import com.example.caseintake.text.QuoteLocator;
import com.example.caseintake.text.QuoteLocator.Span;
import com.example.caseintake.text.SourceText;
import com.example.caseintake.validation.InvalidValueException;
import com.example.caseintake.validation.ValueNormalizer;
import com.example.caseintake.validation.ValueValidator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the model's claim about one field into a result a reviewer can trust, or into unknown with the reason.
 * Order of checks: status; value shape (normalise); catalog validation; every quote located in the source;
 * confidence present, then capped by {@link ConfidencePolicy}. Any failure gives unknown and a note; nothing is
 * repaired by guessing.
 */
public final class FieldResolver {

    static final String NOT_COVERED = "Not covered by the model response (for example a field added to the catalog after recording).";

    private final ValueNormalizer normalizer;
    private final ValueValidator validator;
    private final ConfidencePolicy confidencePolicy;

    public FieldResolver(ValueNormalizer normalizer, ValueValidator validator, ConfidencePolicy confidencePolicy) {
        this.normalizer = normalizer;
        this.validator = validator;
        this.confidencePolicy = confidencePolicy;
    }

    public FieldResult resolve(FieldDefinition field, ModelField claim, SourceText source, PageMarkers pages) {
        if (claim == null) {
            return FieldResult.unknown(NOT_COVERED);
        }
        Optional<FieldStatus> status = FieldStatus.fromJson(claim.status());
        if (status.isEmpty()) {
            return FieldResult.unknown("The model gave an invalid status " + claim.status() + ".");
        }
        return switch (status.get()) {
            case UNKNOWN -> FieldResult.unknown(claim.note());
            case CONFLICT -> FieldResult.conflict(conflictNote(claim));
            case EXTRACTED -> resolveExtracted(field, claim, source, pages);
        };
    }

    private FieldResult resolveExtracted(FieldDefinition field, ModelField claim, SourceText source, PageMarkers pages) {
        Object value;
        try {
            value = normalizer.normalize(field, claim.value());
        } catch (InvalidValueException e) {
            return FieldResult.unknown("Model value " + describe(claim.value()) + " rejected: " + e.getMessage() + ".");
        }
        Optional<String> invalid = validator.validate(field, value);
        if (invalid.isPresent()) {
            return FieldResult.unknown("Model value " + describe(value) + " rejected by the catalog: " + invalid.get() + ".");
        }

        List<Provenance> provenance = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> seenSpans = new LinkedHashSet<>();
        for (String quote : claim.quotes()) {
            Optional<List<Span>> spans = QuoteLocator.locate(source, quote);
            if (spans.isEmpty()) {
                missing.add(quote);
                continue;
            }
            for (Span span : spans.get()) {
                if (seenSpans.add(span.start() + ":" + span.end())) {
                    provenance.add(new Provenance(source.slice(span.start(), span.end()), span.start(), span.end(),
                            pages.pageAt(span.start()).orElse(null)));
                }
            }
        }
        if (provenance.isEmpty()) {
            String why = claim.quotes().isEmpty() ? "no quote was given" : "no quote was found in the document: " + missing;
            return FieldResult.unknown("Model value " + describe(value) + " dropped because " + why + ".");
        }

        List<String> notes = new ArrayList<>();
        if (claim.note() != null) {
            notes.add(claim.note());
        }
        if (!missing.isEmpty()) {
            notes.add("Quotes not found in the document and left out: " + missing + ".");
        }
        Confidence confidence = Confidence.fromJson(claim.confidence()).orElseGet(() -> {
            notes.add("The model gave no valid confidence; set to low.");
            return Confidence.LOW;
        });
        Optional<ConfidencePolicy.Cap> cap = confidencePolicy.capFor(field, value, provenance.stream().map(Provenance::quote).toList());
        if (cap.isPresent() && confidence.compareTo(cap.get().max()) > 0) {
            confidence = confidence.cappedAt(cap.get().max());
            notes.add(cap.get().reason());
        }
        return FieldResult.extracted(value, confidence, provenance, notes.isEmpty() ? null : String.join(" ", notes));
    }

    private static String conflictNote(ModelField claim) {
        StringBuilder note = new StringBuilder(claim.note() == null ? "The document states incompatible values." : claim.note());
        if (!claim.quotes().isEmpty()) {
            note.append(" Quotes: ").append(claim.quotes());
        }
        return note.toString();
    }

    private static String describe(Object value) {
        return value instanceof String s ? "'" + s + "'" : String.valueOf(value);
    }
}
