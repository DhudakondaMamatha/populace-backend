package com.populace.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Report showing hours worked by staff members.
 */
public record StaffHoursReportDto(
    int totalStaffWithHours,
    BigDecimal totalWorkedHours,
    BigDecimal totalOvertimeHours,
    BigDecimal averageHoursPerStaff,
    List<StaffHoursEntry> staffEntries
) {
    public record StaffHoursEntry(
        Long staffId,
        String staffName,
        String employeeCode,
        BigDecimal workedHours,
        BigDecimal overtimeHours,
        BigDecimal totalHours
    ) {}

    public static StaffHoursReportDto empty() {
        return new StaffHoursReportDto(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }
}
