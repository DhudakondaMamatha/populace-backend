package com.populace.compensation.validation;

import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationUpdateRequest;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.domain.StaffCompensation;
import com.populace.domain.enums.CompensationType;
import com.populace.repository.StaffCompensationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Validates compensation data before persistence.
 * Enforces business rules for hourly and monthly compensation.
 */
@Component
public class CompensationValidator {

    private final StaffCompensationRepository compensationRepository;

    public CompensationValidator(StaffCompensationRepository compensationRepository) {
        this.compensationRepository = compensationRepository;
    }

    /**
     * Validate a create request before saving.
     *
     * @param staffId the staff member ID
     * @param request the create request
     * @throws CompensationValidationException if validation fails
     */
    public void validateCreateRequest(Long staffId, CompensationCreateRequest request) {
        CompensationValidationException.Builder errors = new CompensationValidationException.Builder();

        // Parse compensation type (default to hourly)
        CompensationType type = parseCompensationType(request.compensationType());

        // Validate effective dates
        validateEffectiveDates(request.effectiveFrom(), request.effectiveTo(), errors);

        // Validate compensation type-specific rules
        validateCompensationTypeRules(type, request.hourlyRate(), request.monthlySalary(), errors);

        // Validate no overlapping active compensation
        validateNoOverlap(staffId, request.roleId(), request.effectiveFrom(),
                request.effectiveTo(), null, errors);

        errors.throwIfErrors();
    }

    /**
     * Validate an update request before saving.
     *
     * @param existing the existing compensation record
     * @param request the update request
     * @throws CompensationValidationException if validation fails
     */
    public void validateUpdateRequest(StaffCompensation existing, CompensationUpdateRequest request) {
        CompensationValidationException.Builder errors = new CompensationValidationException.Builder();

        // Determine effective values after update
        LocalDate effectiveFrom = request.effectiveFrom() != null
                ? request.effectiveFrom() : existing.getEffectiveFrom();
        LocalDate effectiveTo = request.effectiveTo() != null
                ? request.effectiveTo() : existing.getEffectiveTo();
        CompensationType type = request.compensationType() != null
                ? parseCompensationType(request.compensationType()) : existing.getCompensationType();
        BigDecimal hourlyRate = request.hourlyRate() != null
                ? request.hourlyRate() : existing.getHourlyRate();
        BigDecimal monthlySalary = request.monthlySalary() != null
                ? request.monthlySalary() : existing.getMonthlySalary();

        // Validate effective dates
        validateEffectiveDates(effectiveFrom, effectiveTo, errors);

        // Validate compensation type-specific rules
        validateCompensationTypeRules(type, hourlyRate, monthlySalary, errors);

        // Validate no overlapping if dates changed
        if (request.effectiveFrom() != null || request.effectiveTo() != null) {
            validateNoOverlap(existing.getStaffId(), existing.getRoleId(),
                    effectiveFrom, effectiveTo, existing.getId(), errors);
        }

        errors.throwIfErrors();
    }

    /**
     * Validate an entity before saving (final check).
     *
     * @param compensation the entity to validate
     * @throws CompensationValidationException if validation fails
     */
    public void validateEntity(StaffCompensation compensation) {
        CompensationValidationException.Builder errors = new CompensationValidationException.Builder();

        validateEffectiveDates(compensation.getEffectiveFrom(),
                compensation.getEffectiveTo(), errors);

        validateCompensationTypeRules(compensation.getCompensationType(),
                compensation.getHourlyRate(), compensation.getMonthlySalary(), errors);

        errors.throwIfErrors();
    }

    private void validateEffectiveDates(LocalDate from, LocalDate to,
                                        CompensationValidationException.Builder errors) {
        if (from == null) {
            errors.addError("effectiveFrom", "Effective from date is required");
            return;
        }

        if (to != null && to.isBefore(from)) {
            errors.addError("effectiveTo",
                    "Effective to date must be on or after effective from date");
        }
    }

    private void validateCompensationTypeRules(CompensationType type, BigDecimal hourlyRate,
                                               BigDecimal monthlySalary,
                                               CompensationValidationException.Builder errors) {
        if (type == CompensationType.hourly) {
            validateHourlyCompensation(hourlyRate, monthlySalary, errors);
        } else if (type == CompensationType.monthly) {
            validateMonthlyCompensation(hourlyRate, monthlySalary, errors);
        }
    }

    private void validateHourlyCompensation(BigDecimal hourlyRate, BigDecimal monthlySalary,
                                            CompensationValidationException.Builder errors) {
        // Hourly rate is required
        if (hourlyRate == null) {
            errors.addError("hourlyRate",
                    "Hourly rate is required for hourly compensation");
        } else if (hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            errors.addError("hourlyRate",
                    "Hourly rate must be greater than zero");
        }

        // Monthly salary must be null for hourly workers
        if (monthlySalary != null) {
            errors.addError("monthlySalary",
                    "Monthly salary must not be set for hourly compensation");
        }
    }

    private void validateMonthlyCompensation(BigDecimal hourlyRate, BigDecimal monthlySalary,
                                             CompensationValidationException.Builder errors) {
        // Monthly salary is required
        if (monthlySalary == null) {
            errors.addError("monthlySalary",
                    "Monthly salary is required for monthly compensation");
        } else if (monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            errors.addError("monthlySalary",
                    "Monthly salary must be greater than zero");
        }

        // Hourly rate is still required (for cost estimation in scheduling)
        // but validation is lenient - we just ensure it's positive if provided
        if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            errors.addError("hourlyRate",
                    "Hourly rate must be greater than zero if provided");
        }
    }

    private void validateNoOverlap(Long staffId, Long roleId, LocalDate from, LocalDate to,
                                   Long excludeId, CompensationValidationException.Builder errors) {
        // Find existing compensation records for same staff + role
        List<StaffCompensation> existing = roleId != null
                ? compensationRepository.findByStaff_IdAndRole_Id(staffId, roleId)
                : compensationRepository.findByStaff_Id(staffId).stream()
                        .filter(c -> c.getRoleId() == null)
                        .toList();

        for (StaffCompensation record : existing) {
            // Skip the record being updated
            if (excludeId != null && excludeId.equals(record.getId())) {
                continue;
            }

            if (dateRangesOverlap(from, to, record.getEffectiveFrom(), record.getEffectiveTo())) {
                String roleText = roleId != null ? " for this role" : "";
                errors.addError("effectiveFrom",
                        "Date range overlaps with existing compensation record" + roleText +
                        " (ID: " + record.getId() + ")");
                break; // Report only first overlap
            }
        }
    }

    private boolean dateRangesOverlap(LocalDate start1, LocalDate end1,
                                      LocalDate start2, LocalDate end2) {
        // Treat null end date as "ongoing" (far future)
        LocalDate effectiveEnd1 = end1 != null ? end1 : LocalDate.of(9999, 12, 31);
        LocalDate effectiveEnd2 = end2 != null ? end2 : LocalDate.of(9999, 12, 31);

        // Ranges overlap if start1 <= end2 AND start2 <= end1
        return !start1.isAfter(effectiveEnd2) && !start2.isAfter(effectiveEnd1);
    }

    private CompensationType parseCompensationType(String type) {
        if (type == null || type.isBlank()) {
            return CompensationType.hourly;
        }
        try {
            return CompensationType.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            return CompensationType.hourly;
        }
    }
}
