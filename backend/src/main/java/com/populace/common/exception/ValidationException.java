package com.populace.common.exception;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends BusinessException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new ArrayList<>();
    }

    public ValidationException(String field, String message) {
        super(message);
        this.fieldErrors = List.of(new FieldError(field, message));
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String context, List<String> errors) {
        super(context + ": " + String.join("; ", errors));
        this.fieldErrors = errors.stream()
            .map(e -> new FieldError(context, e))
            .toList();
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public List<String> getErrors() {
        return fieldErrors.stream()
            .map(FieldError::message)
            .toList();
    }

    public record FieldError(String field, String message) {}
}
