package com.populace.shift.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateShiftsFromTemplateRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
