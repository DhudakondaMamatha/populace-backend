package com.populace.staff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Staff data transfer object with full details.
 * Roles are returned as summary DTOs containing ID, name, and skill level.
 * Sites are returned as summary DTOs containing ID and name.
 */
public record StaffDto(
    Long id,
    String employeeCode,
    String firstName,
    String lastName,
    String email,
    String phone,
    String secondaryPhone,
    String employmentStatus,
    String employmentType,
    LocalDate hireDate,
    LocalDate terminationDate,
    // Roles now include skill level (L1/L2/L3) per role
    List<RoleSummaryDto> roles,
    List<SiteSummaryDto> sites,
    // Deprecated: Skill levels are now per-role in roles[].skillLevel
    List<String> competenceLevels,
    // Version for optimistic locking
    Long version,
    // Personal constraint fields
    BigDecimal minHoursPerDay,
    BigDecimal maxHoursPerDay,
    BigDecimal minHoursPerWeek,
    BigDecimal maxHoursPerWeek,
    BigDecimal minHoursPerMonth,
    BigDecimal maxHoursPerMonth,
    Integer minDaysOffPerWeek,
    Integer maxSitesPerDay,
    // Mandatory leave enforcement fields
    Integer mustGoOnLeaveAfterDays,
    Integer accruesOneDayLeaveAfterDays,
    LocalDate lastWorkedDate,
    Integer consecutiveWorkDays,
    Integer accruedMandatoryLeaveDays,
    String allocationReadiness
) {
    /**
     * Backward compatibility: get role names as list.
     */
    public List<String> getRoleNames() {
        return roles != null ? roles.stream().map(RoleSummaryDto::name).toList() : List.of();
    }

    /**
     * Backward compatibility: get site names as list.
     */
    public List<String> getSiteNames() {
        return sites != null ? sites.stream().map(SiteSummaryDto::name).toList() : List.of();
    }

    /**
     * Get role IDs for assignment operations.
     */
    public List<Long> getRoleIds() {
        return roles != null ? roles.stream().map(RoleSummaryDto::id).toList() : List.of();
    }

    /**
     * Get site IDs for assignment operations.
     */
    public List<Long> getSiteIds() {
        return sites != null ? sites.stream().map(SiteSummaryDto::id).toList() : List.of();
    }
}
