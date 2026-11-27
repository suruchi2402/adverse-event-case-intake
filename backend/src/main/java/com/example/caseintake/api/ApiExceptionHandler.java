package com.example.caseintake.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.util.Map;

/** Errors as small JSON bodies the UI can show: {"error": "..."}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(DocumentNotFoundException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(UncheckedIOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> unreadable(UncheckedIOException e) {
        return Map.of("error", e.getMessage());
    }
}
