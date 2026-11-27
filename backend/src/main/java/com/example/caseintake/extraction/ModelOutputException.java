package com.example.caseintake.extraction;

/** The model's answer cannot be read as the expected JSON structure. */
public class ModelOutputException extends RuntimeException {
    public ModelOutputException(String message) {
        super(message);
    }
}
