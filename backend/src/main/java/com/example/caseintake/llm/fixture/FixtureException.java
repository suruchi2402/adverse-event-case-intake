package com.example.caseintake.llm.fixture;

import com.example.caseintake.llm.LlmException;

/** A recording is missing, unreadable, or was made for a different version of the document. */
public class FixtureException extends LlmException {
    public FixtureException(String message) {
        super(message);
    }
}
