package com.populace.site.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record OperatingHoursRequest(
    @NotNull(message = "Day of week is required")
    @Min(value = 0, message = "Day of week must be between 0 and 6")
    @Max(value = 6, message = "Day of week must be between 0 and 6")
    Integer dayOfWeek,

    boolean isClosed,

    LocalTime openTime,

    LocalTime closeTime
) {}
