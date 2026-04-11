package com.populace.compensation.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationUpdateRequest;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.compensation.validation.CompensationValidator;
import com.populace.domain.Role;
import com.populace.domain.StaffCompensation;
import com.populace.domain.StaffMember;
import com.populace.domain.enums.CompensationType;
import com.populace.repository.RoleRepository;
import com.populace.repository.StaffCompensationRepository;
import com.populace.repository.StaffMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing staff compensation records.
 * Supports both hourly and monthly compensation types.
 * All data is validated before persistence.
 */
@Service
public class StaffCompensationService {

    private final StaffCompensationRepository compensationRepository;
    private final StaffMemberRepository staffRepository;
    private final RoleRepository roleRepository;
    private final CompensationValidator validator;

    public StaffCompensationService(
            StaffCompensationRepository compensationRepository,
            StaffMemberRepository staffRepository,
            RoleRepository roleRepository,
            CompensationValidator validator) {
        this.compensationRepository = compensationRepository;
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.validator = validator;
    }

    /**
     * Get all compensation records for a staff member.
     */
    @Transactional(readOnly = true)
    public List<CompensationDto> getCompensationHistory(Long staffId) {
        return compensationRepository.findByStaff_Id(staffId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get the currently active compensation for a staff member.
     * Returns the most recent active record, preferring role-specific over general.
     */
    @Transactional(readOnly = true)
    public Optional<CompensationDto> getCurrentCompensation(Long staffId) {
        LocalDate today = LocalDate.now();
        List<StaffCompensation> activeRecords = compensationRepository
                .findActiveByStaffIdAndDate(staffId, today);

        if (activeRecords.isEmpty()) {
            return Optional.empty();
        }

        // Return the first (most recent) active record
        return Optional.of(toDto(activeRecords.get(0)));
    }

    /**
     * Get the currently active compensation for a staff member and specific role.
     */
    @Transactional(readOnly = true)
    public Optional<CompensationDto> getCurrentCompensationForRole(Long staffId, Long roleId) {
        LocalDate today = LocalDate.now();
        return compensationRepository
                .findActiveByStaffIdAndRoleIdAndDate(staffId, roleId, today)
                .map(this::toDto);
    }

    /**
     * Create a new compensation record for a staff member.
     *
     * @throws CompensationValidationException if validation fails
     * @throws ResourceNotFoundException if staff or role not found
     */
    @Transactional
    public CompensationDto createCompensation(Long staffId, CompensationCreateRequest request) {
        // Validate request before any database operations
        validator.validateCreateRequest(staffId, request);

        StaffMember staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        Role role = null;
        if (request.roleId() != null) {
            role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", request.roleId()));
        }

        StaffCompensation compensation = new StaffCompensation();
        compensation.setStaff(staff);
        compensation.setRole(role);
        compensation.setEffectiveFrom(request.effectiveFrom());
        compensation.setEffectiveTo(request.effectiveTo());

        // Set compensation type
        CompensationType type = parseCompensationType(request.compensationType());
        compensation.setCompensationType(type);

        // Set rate/salary based on type
        if (type == CompensationType.hourly) {
            compensation.setHourlyRate(request.hourlyRate());
            compensation.setMonthlySalary(null);
        } else {
            compensation.setMonthlySalary(request.monthlySalary());
            compensation.setHourlyRate(request.hourlyRate() != null
                    ? request.hourlyRate()
                    : deriveHourlyRateFromSalary(request.monthlySalary()));
        }

        // Final validation before save
        validator.validateEntity(compensation);

        compensation = compensationRepository.save(compensation);
        return toDto(compensation);
    }

    /**
     * Update an existing compensation record.
     *
     * @throws CompensationValidationException if validation fails
     * @throws ResourceNotFoundException if compensation record not found
     */
    @Transactional
    public CompensationDto updateCompensation(Long compensationId, CompensationUpdateRequest request) {
        StaffCompensation compensation = compensationRepository.findById(compensationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", compensationId));

        // Validate request against existing record
        validator.validateUpdateRequest(compensation, request);

        // Apply updates
        if (request.effectiveFrom() != null) {
            compensation.setEffectiveFrom(request.effectiveFrom());
        }
        if (request.effectiveTo() != null) {
            compensation.setEffectiveTo(request.effectiveTo());
        }

        // Handle compensation type change
        if (request.compensationType() != null) {
            CompensationType newType = parseCompensationType(request.compensationType());
            compensation.setCompensationType(newType);

            // Clear monthly salary if switching to hourly
            if (newType == CompensationType.hourly) {
                compensation.setMonthlySalary(null);
            }
        }

        // Apply rate/salary updates
        if (request.hourlyRate() != null) {
            compensation.setHourlyRate(request.hourlyRate());
        }
        if (request.monthlySalary() != null) {
            compensation.setMonthlySalary(request.monthlySalary());
        }

        // Final validation before save
        validator.validateEntity(compensation);

        compensation = compensationRepository.save(compensation);
        return toDto(compensation);
    }

    /**
     * End a compensation record by setting its effective end date.
     *
     * @throws CompensationValidationException if end date is before start date
     * @throws ResourceNotFoundException if compensation record not found
     */
    @Transactional
    public void endCompensation(Long compensationId, LocalDate endDate) {
        StaffCompensation compensation = compensationRepository.findById(compensationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", compensationId));

        if (endDate.isBefore(compensation.getEffectiveFrom())) {
            throw new CompensationValidationException("effectiveTo",
                    "End date cannot be before the start date");
        }

        compensation.setEffectiveTo(endDate);
        compensationRepository.save(compensation);
    }

    /**
     * Check if a staff member is paid hourly.
     */
    @Transactional(readOnly = true)
    public boolean isHourlyWorker(Long staffId) {
        return getCurrentCompensation(staffId)
                .map(dto -> "hourly".equals(dto.compensationType()))
                .orElse(true); // Default to hourly if no record found
    }

    /**
     * Check if a staff member is paid monthly salary.
     */
    @Transactional(readOnly = true)
    public boolean isSalariedWorker(Long staffId) {
        return getCurrentCompensation(staffId)
                .map(dto -> "monthly".equals(dto.compensationType()))
                .orElse(false);
    }

    /**
     * Get the effective hourly rate for a staff member.
     * For hourly workers, returns the hourly rate.
     * For salaried workers, returns null (use monthlySalary instead).
     */
    @Transactional(readOnly = true)
    public BigDecimal getEffectiveHourlyRate(Long staffId) {
        return getCurrentCompensation(staffId)
                .filter(dto -> "hourly".equals(dto.compensationType()))
                .map(CompensationDto::hourlyRate)
                .orElse(null);
    }

    /**
     * Get the monthly salary for a salaried staff member.
     * Returns null for hourly workers.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMonthlySalary(Long staffId) {
        return getCurrentCompensation(staffId)
                .filter(dto -> "monthly".equals(dto.compensationType()))
                .map(CompensationDto::monthlySalary)
                .orElse(null);
    }

    /**
     * Create or update compensation for a staff member.
     * If an active compensation record exists, it is updated.
     * Otherwise, a new record is created.
     *
     * @param staffId The staff member ID
     * @param compensationType The compensation type (hourly or monthly)
     * @param hourlyRate The hourly rate (required for hourly type)
     * @param monthlySalary The monthly salary (required for monthly type)
     */
    @Transactional
    public void upsertCompensation(Long staffId, String compensationType,
                                   BigDecimal hourlyRate, BigDecimal monthlySalary) {
        if (compensationType == null || compensationType.isBlank()) {
            return;
        }

        String normalizedType = compensationType.toLowerCase().trim();
        BigDecimal rate = "hourly".equals(normalizedType) ? hourlyRate : monthlySalary;

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            // Skip compensation upsert: invalid rate for type
            return;
        }

        Optional<StaffCompensation> existing = compensationRepository.findActiveByStaffId(staffId);

        if (existing.isPresent()) {
            updateExistingCompensation(existing.get(), normalizedType, hourlyRate, monthlySalary);
        } else {
            createNewCompensation(staffId, normalizedType, hourlyRate, monthlySalary);
        }
    }

    private void updateExistingCompensation(StaffCompensation compensation,
                                            String type, BigDecimal hourlyRate, BigDecimal monthlySalary) {
        CompensationType compType = parseCompensationType(type);
        compensation.setCompensationType(compType);

        if ("hourly".equals(type)) {
            compensation.setHourlyRate(hourlyRate);
            compensation.setMonthlySalary(null);
        } else {
            compensation.setHourlyRate(hourlyRate != null ? hourlyRate : deriveHourlyRateFromSalary(monthlySalary));
            compensation.setMonthlySalary(monthlySalary);
        }

        compensation.setEffectiveFrom(LocalDate.now());
        compensationRepository.save(compensation);
    }

    private void createNewCompensation(Long staffId, String type,
                                       BigDecimal hourlyRate, BigDecimal monthlySalary) {
        StaffMember staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        StaffCompensation compensation = new StaffCompensation();
        compensation.setStaff(staff);
        compensation.setCompensationType(parseCompensationType(type));
        compensation.setEffectiveFrom(LocalDate.now());

        if ("hourly".equals(type)) {
            compensation.setHourlyRate(hourlyRate);
        } else {
            compensation.setHourlyRate(hourlyRate != null ? hourlyRate : deriveHourlyRateFromSalary(monthlySalary));
            compensation.setMonthlySalary(monthlySalary);
        }

        compensationRepository.save(compensation);
    }

    /**
     * Derive an estimated hourly rate from monthly salary.
     * Based on standard monthly hours (173.33).
     */
    private BigDecimal deriveHourlyRateFromSalary(BigDecimal monthlySalary) {
        if (monthlySalary == null) {
            return new BigDecimal("20.00"); // Fallback default
        }
        // 173.33 = 40 hours/week × 52 weeks / 12 months
        return monthlySalary.divide(new BigDecimal("173.33"), 2, java.math.RoundingMode.HALF_UP);
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

    private CompensationDto toDto(StaffCompensation compensation) {
        String roleName = compensation.getRole() != null ? compensation.getRole().getName() : null;
        return new CompensationDto(
                compensation.getId(),
                compensation.getStaffId(),
                compensation.getRoleId(),
                roleName,
                compensation.getHourlyRate(),
                compensation.getEffectiveFrom(),
                compensation.getEffectiveTo(),
                compensation.getCompensationType().name(),
                compensation.getMonthlySalary(),
                compensation.isCurrentlyActive()
        );
    }
}
