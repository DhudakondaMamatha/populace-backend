package com.populace.staff.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a single row from the bulk upload CSV file.
 * All fields are strings initially and validated/converted during processing.
 */
public record BulkStaffRow(
    int rowNumber,

    // Identity
    String employeeCode,
    String firstName,
    String lastName,
    String email,
    String phone,
    String employmentType,

    // Assignments
    List<String> roles,
    List<String> sites,
    List<String> competenceLevels,
    String primaryRole,

    // Compensation
    String compensationType,
    String hourlyRate,
    String monthlySalary,

    // Work hours
    String minHoursPerDay,
    String maxHoursPerDay,
    String minHoursPerMonth,
    String maxHoursPerMonth,
    String minDaysOffPerWeek,
    String maxSitesPerDay,
    String minHoursPerWeek,
    String maxHoursPerWeek,

    // Mandatory leave
    String mustGoOnLeaveAfterDays,
    String accruesOneDayLeaveAfterDays,

    // Break overrides (optional, applied to all role assignments)
    String minBreakMinutes,
    String maxBreakMinutes,
    String minWorkMinutesBeforeBreak,
    String maxContinuousWorkMinutes
) {
    /**
     * Parsed and validated version of the row.
     */
    public record Validated(
        int rowNumber,

        // Identity
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        String employmentType,

        // Assignments
        List<Long> roleIds,
        List<Long> siteIds,
        List<String> competenceLevels,
        Long primaryRoleId,

        // Compensation
        String compensationType,
        BigDecimal hourlyRate,
        BigDecimal monthlySalary,

        // Work hours
        BigDecimal minHoursPerDay,
        BigDecimal maxHoursPerDay,
        BigDecimal minHoursPerMonth,
        BigDecimal maxHoursPerMonth,
        Integer minDaysOffPerWeek,
        Integer maxSitesPerDay,
        BigDecimal minHoursPerWeek,
        BigDecimal maxHoursPerWeek,

        // Mandatory leave
        Integer mustGoOnLeaveAfterDays,
        Integer accruesOneDayLeaveAfterDays,

        // Break overrides (optional, applied to all role assignments)
        Integer minBreakMinutes,
        Integer maxBreakMinutes,
        Integer minWorkMinutesBeforeBreak,
        Integer maxContinuousWorkMinutes
    ) {}
}
