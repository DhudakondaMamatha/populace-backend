package com.populace.shift.dto;

import java.util.List;

public record BulkShiftCreateResponse(
    int shiftsCreated,
    int shiftsSkipped,
    List<ShiftDto> shifts,
    List<String> skippedDates
) {
    public static BulkShiftCreateResponse of(List<ShiftDto> created, List<String> skipped) {
        return new BulkShiftCreateResponse(
            created.size(),
            skipped.size(),
            created,
            skipped
        );
    }
}
