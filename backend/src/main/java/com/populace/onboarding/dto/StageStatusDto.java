package com.populace.onboarding.dto;

public record StageStatusDto(
    String code,
    String name,
    int displayOrder,
    boolean completed,
    String message
) {
    public static StageStatusDto from(String code, String name, int displayOrder,
                                       boolean completed, String message) {
        return new StageStatusDto(code, name, displayOrder, completed, message);
    }
}
