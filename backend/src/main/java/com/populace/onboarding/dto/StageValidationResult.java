package com.populace.onboarding.dto;

import java.util.Map;

public record StageValidationResult(
    boolean complete,
    String message,
    Map<String, Object> details
) {
    public static StageValidationResult success(String message) {
        return new StageValidationResult(true, message, Map.of());
    }

    public static StageValidationResult failure(String message) {
        return new StageValidationResult(false, message, Map.of());
    }

    public static StageValidationResult withDetails(boolean complete, String message, Map<String, Object> details) {
        return new StageValidationResult(complete, message, details);
    }
}
