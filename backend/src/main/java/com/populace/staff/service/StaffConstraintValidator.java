package com.populace.staff.service;

import com.populace.common.exception.ValidationException;
import com.populace.domain.Business;
import com.populace.repository.BusinessRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates staff work constraint fields.
 *
 * Enforces:
 * - minHoursPerDay > 0
 * - maxHoursPerDay >= minHoursPerDay
 * - maxHoursPerDay <= business.maxDailyHoursCap
 * - minHoursPerWeek <= maxHoursPerWeek
 * - maxHoursPerMonth > 0
 * - minDaysOffPerWeek between 0 and 7
 * - maxSitesPerDay >= 1
 */
@Component
public class StaffConstraintValidator {

    private final BusinessRepository businessRepository;

    public StaffConstraintValidator(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public void ensureConstraintsAreWithinLimits(
            Long businessId,
            BigDecimal minHoursPerDay,
            BigDecimal maxHoursPerDay,
            BigDecimal minHoursPerMonth,
            BigDecimal maxHoursPerMonth,
            Integer minDaysOffPerWeek,
            Integer maxSitesPerDay,
            BigDecimal minHoursPerWeek,
            BigDecimal maxHoursPerWeek) {

        List<String> errors = new ArrayList<>();

        if (minHoursPerDay == null) {
            errors.add("minHoursPerDay is required");
        } else if (minHoursPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("minHoursPerDay must be greater than 0");
        }

        if (maxHoursPerDay == null) {
            errors.add("maxHoursPerDay is required");
        } else if (maxHoursPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("maxHoursPerDay must be greater than 0");
        }

        if (minHoursPerDay != null && maxHoursPerDay != null) {
            if (maxHoursPerDay.compareTo(minHoursPerDay) < 0) {
                errors.add("maxHoursPerDay must be >= minHoursPerDay");
            }
        }

        if (maxHoursPerMonth == null) {
            errors.add("maxHoursPerMonth is required");
        } else if (maxHoursPerMonth.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("maxHoursPerMonth must be greater than 0");
        }

        if (minHoursPerMonth != null && maxHoursPerMonth != null) {
            if (maxHoursPerMonth.compareTo(minHoursPerMonth) < 0) {
                errors.add("maxHoursPerMonth must be >= minHoursPerMonth");
            }
        }

        // Weekly hours validation
        if (minHoursPerWeek != null && maxHoursPerWeek != null) {
            if (minHoursPerWeek.compareTo(maxHoursPerWeek) > 0) {
                errors.add("minHoursPerWeek must be <= maxHoursPerWeek");
            }
        }

        if (minDaysOffPerWeek == null) {
            errors.add("minDaysOffPerWeek is required");
        } else if (minDaysOffPerWeek < 0 || minDaysOffPerWeek > 7) {
            errors.add("minDaysOffPerWeek must be between 0 and 7");
        }

        if (maxSitesPerDay == null) {
            errors.add("maxSitesPerDay is required");
        } else if (maxSitesPerDay < 1) {
            errors.add("maxSitesPerDay must be >= 1");
        }

        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalStateException("Business not found: " + businessId));

        if (maxHoursPerDay != null && business.getMaxDailyHoursCap() != null) {
            if (maxHoursPerDay.compareTo(business.getMaxDailyHoursCap()) > 0) {
                errors.add(String.format(
                    "maxHoursPerDay (%.2f) cannot exceed system cap (%.2f)",
                    maxHoursPerDay, business.getMaxDailyHoursCap()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Staff constraint validation failed", errors);
        }
    }

    public void ensureConstraintsAreWithinLimitsForUpdate(
            Long businessId,
            BigDecimal minHoursPerDay,
            BigDecimal maxHoursPerDay,
            BigDecimal minHoursPerMonth,
            BigDecimal maxHoursPerMonth,
            Integer minDaysOffPerWeek,
            Integer maxSitesPerDay,
            BigDecimal minHoursPerWeek,
            BigDecimal maxHoursPerWeek,
            BigDecimal existingMinHoursPerDay,
            BigDecimal existingMaxHoursPerDay,
            BigDecimal existingMinHoursPerMonth,
            BigDecimal existingMaxHoursPerMonth,
            Integer existingMinDaysOffPerWeek,
            Integer existingMaxSitesPerDay,
            BigDecimal existingMinHoursPerWeek,
            BigDecimal existingMaxHoursPerWeek) {

        BigDecimal effectiveMinHoursPerDay = minHoursPerDay != null ? minHoursPerDay : existingMinHoursPerDay;
        BigDecimal effectiveMaxHoursPerDay = maxHoursPerDay != null ? maxHoursPerDay : existingMaxHoursPerDay;
        BigDecimal effectiveMinHoursPerMonth = minHoursPerMonth != null ? minHoursPerMonth : existingMinHoursPerMonth;
        BigDecimal effectiveMaxHoursPerMonth = maxHoursPerMonth != null ? maxHoursPerMonth : existingMaxHoursPerMonth;
        Integer effectiveMinDaysOffPerWeek = minDaysOffPerWeek != null ? minDaysOffPerWeek : existingMinDaysOffPerWeek;
        Integer effectiveMaxSitesPerDay = maxSitesPerDay != null ? maxSitesPerDay : existingMaxSitesPerDay;
        BigDecimal effectiveMinHoursPerWeek = minHoursPerWeek != null ? minHoursPerWeek : existingMinHoursPerWeek;
        BigDecimal effectiveMaxHoursPerWeek = maxHoursPerWeek != null ? maxHoursPerWeek : existingMaxHoursPerWeek;

        ensureConstraintsAreWithinLimits(
            businessId,
            effectiveMinHoursPerDay,
            effectiveMaxHoursPerDay,
            effectiveMinHoursPerMonth,
            effectiveMaxHoursPerMonth,
            effectiveMinDaysOffPerWeek,
            effectiveMaxSitesPerDay,
            effectiveMinHoursPerWeek,
            effectiveMaxHoursPerWeek
        );
    }
}
