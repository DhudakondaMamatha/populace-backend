package com.populace.business.dto;

import java.math.BigDecimal;

public record BusinessSettingsDto(
    Long id,
    String name,
    int coverageWindowMinutes,
    BigDecimal toleranceOverPercentage,
    BigDecimal toleranceUnderPercentage,
    BigDecimal maxWeeklyHoursCap,
    Integer maxConsecutiveDays,
    Integer minRestBetweenShiftsHours,
    Integer minBreakDurationMinutes,
    Integer maxContinuousWorkMinutes,
    Integer minWorkBeforeBreakMinutes,
    Integer defaultMaxBreaksPerShift
) {}
