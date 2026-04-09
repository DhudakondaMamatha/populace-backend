package com.populace.staff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating staff work parameters.
 * All fields are optional - only non-null values will be applied.
 */
public record WorkParametersUpdateRequest(
    BigDecimal minHoursPerDay,
    BigDecimal maxHoursPerDay,
    BigDecimal minHoursPerWeek,
    BigDecimal maxHoursPerWeek,
    Integer minDaysOffPerWeek,
    BigDecimal minHoursPerMonth,
    BigDecimal maxHoursPerMonth,
    Integer maxSitesPerDay,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Integer mustGoOnLeaveAfterDays,
    Integer accruesOneDayLeaveAfterDays
) {}
