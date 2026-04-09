package com.populace.compensation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to create a new compensation record.
 */
public record CompensationCreateRequest(
        Long roleId,
        BigDecimal hourlyRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String compensationType,
        BigDecimal monthlySalary
) {}
