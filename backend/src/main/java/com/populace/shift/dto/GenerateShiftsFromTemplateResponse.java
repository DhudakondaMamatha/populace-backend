package com.populace.shift.dto;

import java.util.List;

public record GenerateShiftsFromTemplateResponse(
    int shiftsCreated,
    int shiftsSkipped,
    List<String> skippedDates
) {
    public static GenerateShiftsFromTemplateResponse of(int created, int skipped, List<String> skippedDates) {
        return new GenerateShiftsFromTemplateResponse(created, skipped, skippedDates);
    }
}
