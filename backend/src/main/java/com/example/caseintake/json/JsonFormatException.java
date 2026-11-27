package com.example.caseintake.json;

/** Text that should be JSON is not, or does not have the expected shape. */
public class JsonFormatException extends RuntimeException {
    public JsonFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
