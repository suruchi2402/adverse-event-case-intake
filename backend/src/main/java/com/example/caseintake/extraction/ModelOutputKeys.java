package com.example.caseintake.extraction;

/** Property names of the JSON the model returns. Shared by the response schema and the parser so they cannot drift. */
final class ModelOutputKeys {
    static final String FLAGS = "flags";
    static final String CASES = "cases";
    static final String LABEL = "label";
    static final String CRITERIA = "minimum_criteria";
    static final String PRESENT = "present";
    static final String QUOTE = "quote";
    static final String FIELDS = "fields";
    static final String STATUS = "status";
    static final String QUOTES = "quotes";
    static final String VALUE = "value";
    static final String CONFIDENCE = "confidence";
    static final String NOTE = "note";

    private ModelOutputKeys() {
    }
}
