package com.populace.staff.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record StaffUpdateRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    String phone,
    String employeeCode,
    String employmentType,
    // Personal constraint fields (optional - only update if provided)
    @DecimalMin("0.0") @DecimalMax("24.0") BigDecimal minHoursPerDay,
    @DecimalMin("0.0") @DecimalMax("24.0") BigDecimal maxHoursPerDay,
    @DecimalMin("0.0") BigDecimal minHoursPerWeek,
    @DecimalMin("0.0") BigDecimal maxHoursPerWeek,
    @DecimalMin("0.0") BigDecimal minHoursPerMonth,
    @DecimalMin("0.0") BigDecimal maxHoursPerMonth,
    @Min(0) @Max(7) Integer minDaysOffPerWeek,
    @Min(1) Integer maxSitesPerDay,
    // Mandatory leave enforcement fields
    @Min(1) Integer mustGoOnLeaveAfterDays,
    @Min(1) Integer accruesOneDayLeaveAfterDays
) {}
