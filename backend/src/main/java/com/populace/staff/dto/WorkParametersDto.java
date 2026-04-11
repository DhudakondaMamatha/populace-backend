package com.populace.staff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for staff work parameters.
 * Displays either staff-specific overrides or business defaults.
 */
public record WorkParametersDto(
    Long id,
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
    boolean isOverride
) {}
