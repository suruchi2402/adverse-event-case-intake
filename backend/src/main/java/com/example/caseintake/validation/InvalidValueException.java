package com.example.caseintake.validation;

/** A model value cannot be read as the field's type. The field becomes unknown, with this message as the reason. */
public class InvalidValueException extends RuntimeException {
    public InvalidValueException(String message) {
        super(message);
    }
}
