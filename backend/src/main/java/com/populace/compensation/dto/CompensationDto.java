package com.populace.compensation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data transfer object for staff compensation information.
 */
public record CompensationDto(
        Long id,
        Long staffId,
        Long roleId,
        String roleName,
        BigDecimal hourlyRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String compensationType,
        BigDecimal monthlySalary,
        boolean isActive
) {
    /**
     * Check if this is an hourly compensation record.
     */
    public boolean isHourly() {
        return "hourly".equals(compensationType);
    }

    /**
     * Check if this is a monthly salary compensation record.
     */
    public boolean isMonthly() {
        return "monthly".equals(compensationType);
    }
}
