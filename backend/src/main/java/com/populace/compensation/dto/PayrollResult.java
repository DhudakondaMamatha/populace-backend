package com.populace.compensation.dto;

import java.math.BigDecimal;

/**
 * Result of a payroll calculation for a staff member.
 */
public record PayrollResult(
        Long staffId,
        String compensationType,
        BigDecimal hoursWorked,
        BigDecimal totalPay,
        String notes
) {
    /**
     * Create a result indicating no compensation record was found.
     */
    public static PayrollResult noCompensationRecord(Long staffId) {
        return new PayrollResult(
                staffId, null, null,
                BigDecimal.ZERO,
                "No active compensation record found"
        );
    }

    /**
     * Create a result indicating the staff member is not salaried.
     */
    public static PayrollResult notSalaried(Long staffId) {
        return new PayrollResult(
                staffId, "hourly", null,
                BigDecimal.ZERO,
                "Staff member is not salaried"
        );
    }

    /**
     * Create a result indicating monthly salary is not configured.
     */
    public static PayrollResult noSalaryConfigured(Long staffId) {
        return new PayrollResult(
                staffId, "monthly", null,
                BigDecimal.ZERO,
                "Monthly salary not configured"
        );
    }

    /**
     * Check if the calculation was successful.
     */
    public boolean isSuccessful() {
        return totalPay != null && totalPay.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this is for an hourly worker.
     */
    public boolean isHourly() {
        return "hourly".equals(compensationType);
    }

    /**
     * Check if this is for a salaried worker.
     */
    public boolean isMonthly() {
        return "monthly".equals(compensationType);
    }
}
