package com.populace.staff.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.StaffMember;
import com.populace.domain.StaffWorkParameters;
import com.populace.domain.WorkParameters;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.StaffWorkParametersRepository;
import com.populace.repository.WorkParametersRepository;
import com.populace.staff.dto.WorkParametersDto;
import com.populace.staff.dto.WorkParametersUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class StaffWorkParametersService {

    private final StaffWorkParametersRepository staffWorkParamsRepository;
    private final WorkParametersRepository workParamsRepository;
    private final StaffMemberRepository staffRepository;

    public StaffWorkParametersService(StaffWorkParametersRepository staffWorkParamsRepository,
                                       WorkParametersRepository workParamsRepository,
                                       StaffMemberRepository staffRepository) {
        this.staffWorkParamsRepository = staffWorkParamsRepository;
        this.workParamsRepository = workParamsRepository;
        this.staffRepository = staffRepository;
    }

    /**
     * Get work parameters for a staff member.
     * Returns staff-specific overrides if they exist, otherwise returns business defaults.
     */
    @Transactional(readOnly = true)
    public WorkParametersDto getWorkParameters(Long businessId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        // Check for staff-specific overrides
        Optional<StaffWorkParameters> staffOverride = staffWorkParamsRepository.findCurrentByStaffId(staffId);

        if (staffOverride.isPresent()) {
            return toDto(staffOverride.get(), true);
        }

        // Fall back to business defaults
        Optional<WorkParameters> businessDefaults = workParamsRepository.findByBusiness_IdAndIsDefaultTrue(businessId);

        if (businessDefaults.isPresent()) {
            return toDto(businessDefaults.get());
        }

        // Return system defaults if no business defaults configured
        return getSystemDefaults();
    }

    /**
     * Update or create staff-specific work parameter overrides.
     */
    @Transactional
    public WorkParametersDto updateWorkParameters(Long businessId, Long staffId, WorkParametersUpdateRequest request) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        validateRequest(request);

        // Find existing override or create new one
        StaffWorkParameters params = staffWorkParamsRepository.findCurrentByStaffId(staffId)
            .orElseGet(() -> {
                StaffWorkParameters newParams = new StaffWorkParameters();
                newParams.setStaff(staff);
                return newParams;
            });

        // Apply updates
        if (request.minHoursPerDay() != null) {
            params.setMinHoursPerDay(request.minHoursPerDay());
        }
        if (request.maxHoursPerDay() != null) {
            params.setMaxHoursPerDay(request.maxHoursPerDay());
        }
        if (request.minHoursPerWeek() != null) {
            params.setMinHoursPerWeek(request.minHoursPerWeek());
        }
        if (request.maxHoursPerWeek() != null) {
            params.setMaxHoursPerWeek(request.maxHoursPerWeek());
        }
        if (request.minDaysOffPerWeek() != null) {
            params.setMinDaysOffPerWeek(request.minDaysOffPerWeek());
        }
        if (request.minHoursPerMonth() != null) {
            params.setMinHoursPerMonth(request.minHoursPerMonth());
        }
        if (request.maxHoursPerMonth() != null) {
            params.setMaxHoursPerMonth(request.maxHoursPerMonth());
        }
        if (request.maxSitesPerDay() != null) {
            params.setMaxSitesPerDay(request.maxSitesPerDay());
        }
        // Update mandatory leave enforcement fields on StaffMember entity
        if (request.mustGoOnLeaveAfterDays() != null) {
            staff.setMustGoOnLeaveAfterDays(request.mustGoOnLeaveAfterDays());
        }
        if (request.accruesOneDayLeaveAfterDays() != null) {
            staff.setAccruesOneDayLeaveAfterDays(request.accruesOneDayLeaveAfterDays());
        }
        staffRepository.save(staff);

        // Set effective dates
        params.setEffectiveFrom(request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now());
        params.setEffectiveTo(request.effectiveTo());

        params = staffWorkParamsRepository.save(params);
        return toDto(params, true);
    }

    /**
     * Delete staff-specific overrides, reverting to business defaults.
     */
    @Transactional
    public void deleteWorkParameters(Long businessId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        staffWorkParamsRepository.findCurrentByStaffId(staffId)
            .ifPresent(staffWorkParamsRepository::delete);
    }

    private void validateRequest(WorkParametersUpdateRequest request) {
        // Validate min <= max for daily hours
        if (request.minHoursPerDay() != null && request.maxHoursPerDay() != null) {
            if (request.minHoursPerDay().compareTo(request.maxHoursPerDay()) > 0) {
                throw new ValidationException("minHoursPerDay", "Minimum hours per day cannot exceed maximum");
            }
        }

        // Validate min <= max for weekly hours
        if (request.minHoursPerWeek() != null && request.maxHoursPerWeek() != null) {
            if (request.minHoursPerWeek().compareTo(request.maxHoursPerWeek()) > 0) {
                throw new ValidationException("minHoursPerWeek", "Minimum hours per week cannot exceed maximum");
            }
        }

        // Validate min <= max for monthly hours
        if (request.minHoursPerMonth() != null && request.maxHoursPerMonth() != null) {
            if (request.minHoursPerMonth().compareTo(request.maxHoursPerMonth()) > 0) {
                throw new ValidationException("minHoursPerMonth", "Minimum hours per month cannot exceed maximum");
            }
        }

        // Validate days off per week
        if (request.minDaysOffPerWeek() != null) {
            if (request.minDaysOffPerWeek() < 0 || request.minDaysOffPerWeek() > 7) {
                throw new ValidationException("minDaysOffPerWeek", "Days off per week must be between 0 and 7");
            }
        }

        // Validate maxSitesPerDay
        if (request.maxSitesPerDay() != null && request.maxSitesPerDay() < 1) {
            throw new ValidationException("maxSitesPerDay", "Maximum sites per day must be at least 1");
        }

        // Validate date range
        if (request.effectiveFrom() != null && request.effectiveTo() != null) {
            if (request.effectiveTo().isBefore(request.effectiveFrom())) {
                throw new ValidationException("effectiveTo", "Effective end date cannot be before start date");
            }
        }
    }

    private WorkParametersDto toDto(StaffWorkParameters params, boolean isOverride) {
        return new WorkParametersDto(
            params.getId(),
            params.getMinHoursPerDay(),
            params.getMaxHoursPerDay(),
            params.getMinHoursPerWeek(),
            params.getMaxHoursPerWeek(),
            params.getMinDaysOffPerWeek(),
            params.getMinHoursPerMonth(),
            params.getMaxHoursPerMonth(),
            params.getMaxSitesPerDay(),
            params.getEffectiveFrom(),
            params.getEffectiveTo(),
            isOverride
        );
    }

    private WorkParametersDto toDto(WorkParameters params) {
        return new WorkParametersDto(
            null,
            params.getMinHoursPerDay(),
            params.getMaxHoursPerDay(),
            params.getMinHoursPerWeek(),
            params.getMaxHoursPerWeek(),
            params.getMinDaysOffPerWeek(),
            params.getMinHoursPerMonth(),
            params.getMaxHoursPerMonth(),
            null,
            null,
            null,
            false
        );
    }

    private WorkParametersDto getSystemDefaults() {
        return new WorkParametersDto(
            null,
            new BigDecimal("4.00"),
            new BigDecimal("12.00"),
            new BigDecimal("20.00"),
            new BigDecimal("48.00"),
            1,
            new BigDecimal("80.00"),
            new BigDecimal("208.00"),
            1,
            null,
            null,
            false
        );
    }
}
