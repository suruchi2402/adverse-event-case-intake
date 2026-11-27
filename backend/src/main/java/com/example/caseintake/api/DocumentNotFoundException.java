package com.example.caseintake.api;

/** No document with this id exists in the configured documents folders. */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String id) {
        super("No document with id '" + id + "'");
    }
}
