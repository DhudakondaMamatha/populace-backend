package com.populace.staff.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified staff creation request that supports the complete data model.
 * This DTO matches the full capability of bulk upload for single staff creation.
 *
 * <h2>Supported Field Categories:</h2>
 * <ul>
 *   <li>Core identity fields</li>
 *   <li>Role and site assignments (multi-select)</li>
 *   <li>Competence levels (multi-select)</li>
 *   <li>Compensation configuration</li>
 *   <li>Work rule parameters (personal constraints)</li>
 * </ul>
 *
 * <h2>Validation Rules:</h2>
 * <ul>
 *   <li>All work parameter fields are required (no hidden defaults)</li>
 *   <li>Personal constraints are validated against Business-level system caps</li>
 *   <li>maxHoursPerDay cannot exceed Business.maxDailyHoursCap</li>
 *   <li>Email format requires valid TLD (e.g., user@domain.com)</li>
 *   <li>Employment type must be: permanent or contract</li>
 *   <li>Compensation type must be: hourly or monthly</li>
 *   <li>Competence levels must be: L1, L2, or L3</li>
 * </ul>
 */
public record UnifiedStaffCreateRequest(
    // Core Identity Fields
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    @Email(message = "Invalid email format")
    String email,

    String phone,

    String employeeCode,

    @NotBlank(message = "Employment type is required")
    String employmentType,

    // Assignments
    List<Long> roleIds,

    List<Long> siteIds,

    List<String> competenceLevels,

    List<RoleAssignment> roleAssignments,

    // Compensation
    String compensationType,

    @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be greater than zero")
    BigDecimal hourlyRate,

    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly salary must be greater than zero")
    BigDecimal monthlySalary,

    // Personal Work Constraints — all required, no hidden defaults
    @NotNull(message = "minHoursPerDay is required")
    @DecimalMin(value = "0.0", message = "Minimum hours per day cannot be negative")
    @DecimalMax(value = "24.0", message = "Minimum hours per day cannot exceed 24")
    BigDecimal minHoursPerDay,

    @NotNull(message = "maxHoursPerDay is required")
    @DecimalMin(value = "0.0", message = "Maximum hours per day cannot be negative")
    @DecimalMax(value = "24.0", message = "Maximum hours per day cannot exceed 24")
    BigDecimal maxHoursPerDay,

    @NotNull(message = "minHoursPerMonth is required")
    @DecimalMin(value = "0.0", message = "Minimum hours per month cannot be negative")
    BigDecimal minHoursPerMonth,

    @NotNull(message = "maxHoursPerMonth is required")
    @DecimalMin(value = "0.0", message = "Maximum hours per month cannot be negative")
    BigDecimal maxHoursPerMonth,

    @NotNull(message = "minDaysOffPerWeek is required")
    @Min(value = 0, message = "Minimum days off per week cannot be negative")
    @Max(value = 7, message = "Minimum days off per week cannot exceed 7")
    Integer minDaysOffPerWeek,

    @NotNull(message = "maxSitesPerDay is required")
    @Min(value = 1, message = "Maximum sites per day must be at least 1")
    Integer maxSitesPerDay,

    @NotNull(message = "minHoursPerWeek is required")
    @DecimalMin(value = "0.0", message = "Minimum hours per week cannot be negative")
    BigDecimal minHoursPerWeek,

    @NotNull(message = "maxHoursPerWeek is required")
    @DecimalMin(value = "0.0", message = "Maximum hours per week cannot be negative")
    BigDecimal maxHoursPerWeek,

    // Mandatory Leave Enforcement
    @Min(value = 1, message = "Must go on leave after days must be at least 1")
    Integer mustGoOnLeaveAfterDays,

    @Min(value = 1, message = "Accrues one day leave after days must be at least 1")
    Integer accruesOneDayLeaveAfterDays
) {
    /**
     * Returns true if compensation data is provided.
     */
    public boolean hasCompensation() {
        return compensationType != null && !compensationType.isBlank();
    }

    /**
     * Returns true if work parameter overrides are provided.
     */
    public boolean hasWorkParameters() {
        return minHoursPerDay != null
            || maxHoursPerDay != null
            || minHoursPerWeek != null
            || maxHoursPerWeek != null
            || minHoursPerMonth != null
            || maxHoursPerMonth != null
            || minDaysOffPerWeek != null
            || maxSitesPerDay != null;
    }

    /**
     * Returns true if competence levels are provided.
     */
    public boolean hasCompetenceLevels() {
        return competenceLevels != null && !competenceLevels.isEmpty();
    }

    /**
     * Returns true if role assignments with competence are provided.
     */
    public boolean hasRoleAssignments() {
        return roleAssignments != null && !roleAssignments.isEmpty();
    }

    /**
     * Returns effective role IDs from either roleAssignments or roleIds.
     * Prefers roleAssignments if both are provided.
     */
    public List<Long> getEffectiveRoleIds() {
        if (hasRoleAssignments()) {
            return roleAssignments.stream()
                .map(RoleAssignment::roleId)
                .toList();
        }
        return roleIds;
    }

    /**
     * Normalizes compensation type to lowercase.
     */
    public String normalizedCompensationType() {
        return compensationType != null ? compensationType.toLowerCase().trim() : null;
    }

    /**
     * Normalizes employment type to lowercase.
     */
    public String normalizedEmploymentType() {
        return employmentType != null ? employmentType.toLowerCase().trim() : null;
    }
}
