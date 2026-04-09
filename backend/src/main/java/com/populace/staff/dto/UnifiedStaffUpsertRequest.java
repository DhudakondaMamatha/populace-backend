package com.populace.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified staff upsert request that supports both create and update operations.
 * If id is null, a new staff member is created. If id is present, the existing staff is updated.
 *
 * <h2>Optimistic Locking:</h2>
 * When updating (id is present), the version field should be provided for optimistic locking.
 * If the version doesn't match the current version in the database, a VERSION_MISMATCH error is returned.
 */
public record UnifiedStaffUpsertRequest(
    // ID and Version for upsert/optimistic locking
    Long id,
    Long version,

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

    String employmentStatus,

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

    // Personal Work Constraints
    @DecimalMin(value = "0.0", message = "Minimum hours per day cannot be negative")
    @Max(value = 24, message = "Minimum hours per day cannot exceed 24")
    BigDecimal minHoursPerDay,

    @DecimalMin(value = "0.0", message = "Maximum hours per day cannot be negative")
    @Max(value = 24, message = "Maximum hours per day cannot exceed 24")
    BigDecimal maxHoursPerDay,

    @DecimalMin(value = "0.0", message = "Minimum hours per month cannot be negative")
    BigDecimal minHoursPerMonth,

    @DecimalMin(value = "0.0", message = "Maximum hours per month cannot be negative")
    BigDecimal maxHoursPerMonth,

    @Min(value = 0, message = "Minimum days off per week cannot be negative")
    @Max(value = 7, message = "Minimum days off per week cannot exceed 7")
    Integer minDaysOffPerWeek,

    @Min(value = 1, message = "Maximum sites per day must be at least 1")
    Integer maxSitesPerDay,

    @DecimalMin(value = "0.0", message = "Minimum hours per week cannot be negative")
    BigDecimal minHoursPerWeek,

    @DecimalMin(value = "0.0", message = "Maximum hours per week cannot be negative")
    BigDecimal maxHoursPerWeek,

    // Mandatory Leave Enforcement
    @Min(value = 1, message = "Must go on leave after days must be at least 1")
    Integer mustGoOnLeaveAfterDays,

    @Min(value = 1, message = "Accrues one day leave after days must be at least 1")
    Integer accruesOneDayLeaveAfterDays
) {
    /**
     * Returns true if this is a create operation (no id provided).
     */
    public boolean isCreate() {
        return id == null;
    }

    /**
     * Returns true if this is an update operation (id provided).
     */
    public boolean isUpdate() {
        return id != null;
    }

    /**
     * Returns true if compensation data is provided.
     */
    public boolean hasCompensation() {
        return compensationType != null && !compensationType.isBlank();
    }

    /**
     * Returns true if role assignments with proficiency are provided.
     */
    public boolean hasRoleAssignments() {
        return roleAssignments != null && !roleAssignments.isEmpty();
    }

    /**
     * Returns true if site assignments are provided.
     */
    public boolean hasSiteAssignments() {
        return siteIds != null && !siteIds.isEmpty();
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
