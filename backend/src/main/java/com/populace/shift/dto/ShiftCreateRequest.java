package com.populace.shift.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftCreateRequest(
    @NotNull Long siteId,
    @NotNull Long roleId,
    @NotNull LocalDate shiftDate,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    Integer breakDurationMinutes,
    Integer staffRequired,
    String notes
) {}
