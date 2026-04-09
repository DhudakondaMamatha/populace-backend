package com.populace.compensation.dto;

import java.math.BigDecimal;

/**
 * Summary of compensation statistics for a business.
 * Used for reporting and analytics.
 */
public record CompensationSummary(
        Long businessId,
        int hourlyStaffCount,
        int salariedStaffCount,
        int totalStaffCount,
        BigDecimal averageHourlyRate,
        BigDecimal totalMonthlySalaryExpense
) {
    /**
     * Get the percentage of staff paid hourly.
     */
    public double hourlyPercentage() {
        if (totalStaffCount == 0) return 0.0;
        return (double) hourlyStaffCount / totalStaffCount * 100;
    }

    /**
     * Get the percentage of staff paid monthly salary.
     */
    public double salariedPercentage() {
        if (totalStaffCount == 0) return 0.0;
        return (double) salariedStaffCount / totalStaffCount * 100;
    }
}
