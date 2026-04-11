package com.populace.compensation.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when compensation data fails validation.
 * Contains specific field errors for clear error reporting.
 */
public class CompensationValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public CompensationValidationException(String message) {
        super(message);
        this.errors = List.of(new FieldError("compensation", message));
    }

    public CompensationValidationException(String field, String message) {
        super(message);
        this.errors = List.of(new FieldError(field, message));
    }

    public CompensationValidationException(List<FieldError> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private static String buildMessage(List<FieldError> errors) {
        if (errors.isEmpty()) {
            return "Compensation validation failed";
        }
        if (errors.size() == 1) {
            return errors.get(0).message();
        }
        return "Compensation validation failed with " + errors.size() + " errors";
    }

    /**
     * Represents a validation error for a specific field.
     */
    public record FieldError(String field, String message) {}

    /**
     * Builder for collecting multiple validation errors.
     */
    public static class Builder {
        private final List<FieldError> errors = new ArrayList<>();

        public Builder addError(String field, String message) {
            errors.add(new FieldError(field, message));
            return this;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public void throwIfErrors() {
            if (hasErrors()) {
                throw new CompensationValidationException(errors);
            }
        }

        public CompensationValidationException build() {
            return new CompensationValidationException(errors);
        }
    }
}
