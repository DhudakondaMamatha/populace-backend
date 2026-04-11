package com.populace.shift.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BulkShiftUpdateRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    List<Long> roleIds,        // null or empty = all roles
    Integer breakDurationMinutes, // null = do not change
    Integer staffRequired         // null = do not change
) {}
