package com.populace.compensation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to update an existing compensation record.
 * All fields are optional; only non-null values are applied.
 */
public record CompensationUpdateRequest(
        BigDecimal hourlyRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String compensationType,
        BigDecimal monthlySalary
) {}
