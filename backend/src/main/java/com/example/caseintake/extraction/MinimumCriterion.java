package com.example.caseintake.extraction;

/** The four minimum elements of a valid case (see GLOSSARY.md). These come from the domain, not the catalog. */
public enum MinimumCriterion {
    PATIENT("patient", "an identifiable patient"),
    REPORTER("reporter", "a reporter"),
    SUSPECT_DRUG("suspect_drug", "a suspect drug"),
    ADVERSE_EVENT("adverse_event", "an adverse event");

    private final String jsonName;
    private final String description;

    MinimumCriterion(String jsonName, String description) {
        this.jsonName = jsonName;
        this.description = description;
    }

    public String jsonName() {
        return jsonName;
    }

    public String description() {
        return description;
    }
}
