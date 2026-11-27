package com.example.caseintake.catalog;

/** The catalog file is missing, unreadable or does not have the expected structure. */
public class CatalogException extends RuntimeException {
    public CatalogException(String message) {
        super(message);
    }

    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
