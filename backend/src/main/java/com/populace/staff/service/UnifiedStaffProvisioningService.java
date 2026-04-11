package com.populace.staff.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.common.util.EmailValidator;
import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.compensation.service.StaffCompensationService;
import com.populace.domain.*;
import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.EmploymentType;
import com.populace.domain.enums.SkillLevel;
import com.populace.repository.BusinessRepository;
import com.populace.repository.RoleRepository;
import com.populace.repository.SiteRepository;
import com.populace.repository.StaffCompensationRepository;
import com.populace.repository.StaffCompetenceLevelRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.StaffRoleRepository;
import com.populace.repository.StaffSiteRepository;
import com.populace.repository.StaffWorkParametersRepository;
import com.populace.staff.contract.StaffValidationConstants;
import com.populace.staff.dto.RoleAssignment;
import com.populace.staff.dto.RoleSummaryDto;
import com.populace.staff.dto.SiteSummaryDto;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Unified service for creating fully provisioned staff members.
 * This service is the single source of truth for staff creation logic,
 * used by both manual creation and bulk upload.
 *
 * A fully provisioned staff member includes:
 * - Core identity fields
 * - Role assignments
 * - Site assignments
 * - Competence levels
 * - Compensation record
 * - Work parameter overrides
 *
 * All operations are performed in a single transaction.
 * If any validation fails, the entire operation is rolled back.
 */
@Service
public class UnifiedStaffProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedStaffProvisioningService.class);

    private final StaffMemberRepository staffRepository;
    private final BusinessRepository businessRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final StaffSiteRepository staffSiteRepository;
    private final StaffCompetenceLevelRepository competenceLevelRepository;
    private final StaffCompensationRepository staffCompensationRepository;
    private final StaffCompensationService compensationService;
    private final StaffConstraintValidator constraintValidator;
    private final StaffWorkParametersRepository staffWorkParametersRepository;

    public UnifiedStaffProvisioningService(
            StaffMemberRepository staffRepository,
            BusinessRepository businessRepository,
            RoleRepository roleRepository,
            SiteRepository siteRepository,
            StaffRoleRepository staffRoleRepository,
            StaffSiteRepository staffSiteRepository,
            StaffCompetenceLevelRepository competenceLevelRepository,
            StaffCompensationRepository staffCompensationRepository,
            StaffCompensationService compensationService,
            StaffConstraintValidator constraintValidator,
            StaffWorkParametersRepository staffWorkParametersRepository) {
        this.staffRepository = staffRepository;
        this.businessRepository = businessRepository;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.staffSiteRepository = staffSiteRepository;
        this.competenceLevelRepository = competenceLevelRepository;
        this.staffCompensationRepository = staffCompensationRepository;
        this.compensationService = compensationService;
        this.constraintValidator = constraintValidator;
        this.staffWorkParametersRepository = staffWorkParametersRepository;
    }

    /**
     * Creates a fully provisioned staff member in a single transaction.
     *
     * @param businessId The business ID
     * @param request The unified creation request
     * @return The created staff DTO
     * @throws ValidationException if any validation fails
     * @throws ResourceNotFoundException if business, role, or site not found
     */
    @Transactional
    public StaffDto createStaffWithFullProvisioning(Long businessId, UnifiedStaffCreateRequest request) {
        log.info("Creating fully provisioned staff member for business {}", businessId);

        // Step 1: Validate all input
        ensureRequestIsValid(businessId, request);

        // Step 2: Get business
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        // Step 3: Create staff member
        StaffMember staff = saveStaffMember(business, request);
        staff = staffRepository.save(staff);
        Long staffId = staff.getId();
        log.debug("Created staff member with ID {}", staffId);

        // Step 4: Create role assignments (prefer roleAssignments over deprecated roleIds)
        if (request.hasRoleAssignments()) {
            saveRoleAssignments(staff, request.roleAssignments());
            log.debug("Created {} role assignments with competence levels", request.roleAssignments().size());
        } else if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            createRoleAssignments(staff, request.roleIds());
            log.debug("Created {} role assignments", request.roleIds().size());
        }

        // Step 5: Create site assignments
        if (request.siteIds() != null && !request.siteIds().isEmpty()) {
            saveSiteAssignments(staff, request.siteIds());
            log.debug("Created {} site assignments", request.siteIds().size());
        }

        // Step 6: Create competence levels
        if (request.hasCompetenceLevels()) {
            createCompetenceLevels(staff, request.competenceLevels());
            log.debug("Created {} competence levels", request.competenceLevels().size());
        }

        // Step 7: Create compensation record
        if (request.hasCompensation()) {
            saveCompensation(staffId, request);
            log.debug("Created compensation record");
        }

        // Step 8: Create work parameters record
        saveWorkParameters(staff, request);
        log.debug("Created work parameters record");

        log.info("Successfully created fully provisioned staff member: {} {}",
            request.firstName(), request.lastName());

        return toDto(staff);
    }

    /**
     * Validates the entire request before any database operations.
     */
    private void ensureRequestIsValid(Long businessId, UnifiedStaffCreateRequest request) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (request.firstName() == null || request.firstName().isBlank()) {
            errors.add("firstName: First name is required");
        }
        if (request.lastName() == null || request.lastName().isBlank()) {
            errors.add("lastName: Last name is required");
        }

        // Validate employment type
        String empType = request.normalizedEmploymentType();
        if (empType == null || !StaffValidationConstants.VALID_EMPLOYMENT_TYPES.contains(empType)) {
            errors.add("employmentType: Must be one of: permanent, contract");
        }

        // Validate email format if provided
        if (request.email() != null && !request.email().isBlank()) {
            if (!isValidEmail(request.email())) {
                errors.add("email: Invalid email format");
            }
            // Check uniqueness
            if (staffRepository.existsByEmailIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(
                    request.email(), businessId)) {
                errors.add("email: Email already exists in this business");
            }
        }

        // Validate phone format if provided
        if (request.phone() != null && !request.phone().isBlank()) {
            if (!StaffValidationConstants.PHONE_PATTERN.matcher(request.phone().trim()).matches()) {
                errors.add("phone: Invalid phone format");
            }
        }

        // Check employee code uniqueness if provided
        if (request.employeeCode() != null && !request.employeeCode().isBlank()) {
            if (staffRepository.existsByEmployeeCodeIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(
                    request.employeeCode(), businessId)) {
                errors.add("employeeCode: Employee code already exists in this business");
            }
        }

        // Validate role assignments (prefer roleAssignments over deprecated roleIds)
        if (request.hasRoleAssignments()) {
            for (RoleAssignment assignment : request.roleAssignments()) {
                if (assignment.roleId() == null) {
                    errors.add("roleAssignments: roleId is required");
                } else if (!roleRepository.existsByIdAndBusiness_Id(assignment.roleId(), businessId)) {
                    errors.add("roleAssignments: Role with ID " + assignment.roleId() + " not found");
                }
                // Validate proficiency level if provided
                if (assignment.proficiencyLevel() != null && !StaffValidationConstants.VALID_PROFICIENCY_LEVELS.contains(assignment.proficiencyLevel())) {
                    errors.add("roleAssignments: Invalid proficiency level '" + assignment.proficiencyLevel() + "' for role " + assignment.roleId() + ". Valid levels: trainee, competent, expert");
                }
            }
        } else if (request.roleIds() != null) {
            // Validate deprecated roleIds
            for (Long roleId : request.roleIds()) {
                if (!roleRepository.existsByIdAndBusiness_Id(roleId, businessId)) {
                    errors.add("roleIds: Role with ID " + roleId + " not found");
                }
            }
        }

        // Validate site IDs exist
        if (request.siteIds() != null) {
            for (Long siteId : request.siteIds()) {
                if (!siteRepository.existsByIdAndBusiness_Id(siteId, businessId)) {
                    errors.add("siteIds: Site with ID " + siteId + " not found");
                }
            }
        }

        // Validate competence levels
        if (request.competenceLevels() != null) {
            for (String level : request.competenceLevels()) {
                if (!StaffValidationConstants.VALID_COMPETENCE_LEVELS.contains(level)) {
                    errors.add("competenceLevels: Invalid level '" + level + "'. Valid levels: L1, L2, L3");
                }
            }
        }

        // Validate compensation
        if (request.hasCompensation()) {
            ensureCompensationIsValid(request, errors);
        }

        // Validate work parameters — all fields required, no defaults
        ensureWorkParametersAreValid(businessId, request, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("validation", String.join("; ", errors));
        }
    }

    private void ensureCompensationIsValid(UnifiedStaffCreateRequest request, List<String> errors) {
        String compType = request.normalizedCompensationType();

        if (!StaffValidationConstants.VALID_COMPENSATION_TYPES.contains(compType)) {
            errors.add("compensationType: Must be one of: hourly, monthly");
            return;
        }

        if ("hourly".equals(compType)) {
            if (request.hourlyRate() == null) {
                errors.add("hourlyRate: Required when compensation type is hourly");
            } else if (request.hourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("hourlyRate: Must be greater than zero");
            }
            if (request.monthlySalary() != null) {
                errors.add("monthlySalary: Should not be set for hourly compensation");
            }
        } else if ("monthly".equals(compType)) {
            if (request.monthlySalary() == null) {
                errors.add("monthlySalary: Required when compensation type is monthly");
            } else if (request.monthlySalary().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("monthlySalary: Must be greater than zero");
            }
        }
    }

    private void ensureWorkParametersAreValid(Long businessId, UnifiedStaffCreateRequest request, List<String> errors) {
        // All work parameter fields are required — no defaults applied
        if (request.minHoursPerDay() == null) {
            errors.add("minHoursPerDay is required");
        }
        if (request.maxHoursPerDay() == null) {
            errors.add("maxHoursPerDay is required");
        }
        if (request.minHoursPerWeek() == null) {
            errors.add("minHoursPerWeek is required");
        }
        if (request.maxHoursPerWeek() == null) {
            errors.add("maxHoursPerWeek is required");
        }
        if (request.minHoursPerMonth() == null) {
            errors.add("minHoursPerMonth is required");
        }
        if (request.maxHoursPerMonth() == null) {
            errors.add("maxHoursPerMonth is required");
        }
        if (request.minDaysOffPerWeek() == null) {
            errors.add("minDaysOffPerWeek is required");
        }
        if (request.maxSitesPerDay() == null) {
            errors.add("maxSitesPerDay is required");
        }
        // Only run constraint validation if all required fields are present
        boolean allPresent = request.minHoursPerDay() != null
            && request.maxHoursPerDay() != null
            && request.minHoursPerMonth() != null
            && request.maxHoursPerMonth() != null
            && request.minDaysOffPerWeek() != null
            && request.maxSitesPerDay() != null;

        if (allPresent) {
            try {
                constraintValidator.ensureConstraintsAreWithinLimits(
                    businessId,
                    request.minHoursPerDay(),
                    request.maxHoursPerDay(),
                    request.minHoursPerMonth(),
                    request.maxHoursPerMonth(),
                    request.minDaysOffPerWeek(),
                    request.maxSitesPerDay(),
                    request.minHoursPerWeek(),
                    request.maxHoursPerWeek()
                );
            } catch (ValidationException e) {
                errors.addAll(e.getErrors());
            }
        }
    }

    private boolean isValidEmail(String email) {
        return EmailValidator.isValid(email);
    }

    private StaffMember saveStaffMember(Business business, UnifiedStaffCreateRequest request) {
        StaffMember staff = new StaffMember();
        staff.setBusiness(business);
        staff.setFirstName(request.firstName().trim());
        staff.setLastName(request.lastName().trim());
        staff.setEmail(request.email() != null ? request.email().trim() : null);
        staff.setPhone(request.phone() != null ? request.phone().trim() : null);
        staff.setEmployeeCode(request.employeeCode() != null ? request.employeeCode().trim() : null);
        staff.setEmploymentType(parseEmploymentType(request.normalizedEmploymentType()));
        staff.setEmploymentStatus(EmploymentStatus.active);

        // Set mandatory leave enforcement fields
        if (request.mustGoOnLeaveAfterDays() != null) {
            staff.setMustGoOnLeaveAfterDays(request.mustGoOnLeaveAfterDays());
        }
        if (request.accruesOneDayLeaveAfterDays() != null) {
            staff.setAccruesOneDayLeaveAfterDays(request.accruesOneDayLeaveAfterDays());
        }

        return staff;
    }

    private void createRoleAssignments(StaffMember staff, List<Long> roleIds) {
        boolean isFirst = true;
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                StaffRole staffRole = new StaffRole();
                staffRole.setStaff(staff);
                staffRole.setRole(role);
                staffRole.setPrimary(isFirst);
                staffRole.setActive(true);
                staffRoleRepository.save(staffRole);
                isFirst = false;
            }
        }
    }

    private void saveRoleAssignments(StaffMember staff, List<RoleAssignment> roleAssignments) {
        boolean hasPrimaryMarked = roleAssignments.stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.isPrimary()));

        boolean isFirst = true;
        for (RoleAssignment assignment : roleAssignments) {
            Role role = roleRepository.findById(assignment.roleId()).orElse(null);
            if (role != null) {
                StaffRole staffRole = new StaffRole();
                staffRole.setStaff(staff);
                staffRole.setRole(role);

                if (hasPrimaryMarked) {
                    staffRole.setPrimary(Boolean.TRUE.equals(assignment.isPrimary()));
                } else {
                    staffRole.setPrimary(isFirst);
                }

                staffRole.setActive(true);
                staffRole.setSkillLevel(assignment.getResolvedSkillLevel());

                // Set break overrides from CSV/API when present
                if (assignment.minBreakMinutes() != null) {
                    staffRole.setMinBreakMinutes(assignment.minBreakMinutes());
                }
                if (assignment.maxBreakMinutes() != null) {
                    staffRole.setMaxBreakMinutes(assignment.maxBreakMinutes());
                }
                if (assignment.minWorkMinutesBeforeBreak() != null) {
                    staffRole.setMinWorkMinutesBeforeBreak(assignment.minWorkMinutesBeforeBreak());
                }
                if (assignment.maxContinuousWorkMinutes() != null) {
                    staffRole.setMaxContinuousWorkMinutes(assignment.maxContinuousWorkMinutes());
                }

                staffRoleRepository.save(staffRole);
                isFirst = false;
            }
        }
    }

    private void saveSiteAssignments(StaffMember staff, List<Long> siteIds) {
        for (Long siteId : siteIds) {
            Site site = siteRepository.findById(siteId).orElse(null);
            if (site != null) {
                StaffSite staffSite = new StaffSite();
                staffSite.setStaff(staff);
                staffSite.setSite(site);
                staffSite.setActive(true);
                staffSiteRepository.save(staffSite);
            }
        }
    }

    private void createCompetenceLevels(StaffMember staff, List<String> levels) {
        Set<String> uniqueLevels = new HashSet<>(levels);
        for (String level : uniqueLevels) {
            StaffCompetenceLevel competenceLevel = new StaffCompetenceLevel(staff, level);
            competenceLevelRepository.save(competenceLevel);
        }
    }

    /**
     * Creates compensation records for EACH assigned role.
     *
     * <p><strong>Important Behavior:</strong></p>
     * <ul>
     *   <li>The SAME rate (hourlyRate or monthlySalary) is applied to ALL roles</li>
     *   <li>Staff without compensation for a role cannot be allocated to shifts for that role</li>
     *   <li>If different rates per role are required, create compensation records separately via API</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Staff with roles [Waiter, Cleaner] and hourlyRate=25.00:
     * - Creates compensation: Waiter @ $25.00/hr
     * - Creates compensation: Cleaner @ $25.00/hr
     * </pre>
     *
     * <p>This is intentional to ensure staff can be allocated to any of their assigned roles
     * immediately after creation. Role-specific rates can be updated later via the
     * compensation management API.</p>
     */
    private void saveCompensation(Long staffId, UnifiedStaffCreateRequest request) {
        List<Long> roleIds = request.getEffectiveRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            // No roles assigned - create general compensation without role
            saveCompensationForRole(staffId, null, request);
            return;
        }

        // Create compensation for EACH role
        for (Long roleId : roleIds) {
            saveCompensationForRole(staffId, roleId, request);
        }

        log.debug("Created compensation for {} roles", roleIds.size());
    }

    private void saveCompensationForRole(Long staffId, Long roleId, UnifiedStaffCreateRequest request) {
        CompensationCreateRequest compRequest = new CompensationCreateRequest(
            roleId,
            request.hourlyRate(),
            LocalDate.now(),
            null,
            request.normalizedCompensationType(),
            request.monthlySalary()
        );

        try {
            compensationService.createCompensation(staffId, compRequest);
        } catch (CompensationValidationException e) {
            throw new ValidationException("compensation", e.getMessage());
        }
    }

    private void saveWorkParameters(StaffMember staff, UnifiedStaffCreateRequest request) {
        StaffWorkParameters params = new StaffWorkParameters();
        params.setStaff(staff);
        params.setEffectiveFrom(LocalDate.now());

        // All values come directly from request — no defaults applied
        params.setMinHoursPerDay(request.minHoursPerDay());
        params.setMaxHoursPerDay(request.maxHoursPerDay());
        params.setMinHoursPerWeek(request.minHoursPerWeek());
        params.setMaxHoursPerWeek(request.maxHoursPerWeek());
        params.setMinHoursPerMonth(request.minHoursPerMonth());
        params.setMaxHoursPerMonth(request.maxHoursPerMonth());
        params.setMinDaysOffPerWeek(request.minDaysOffPerWeek());
        params.setMaxSitesPerDay(request.maxSitesPerDay());

        staffWorkParametersRepository.save(params);
    }

    private EmploymentType parseEmploymentType(String type) {
        if (type == null) {
            throw new ValidationException("employmentType", "Employment type is required");
        }
        try {
            return EmploymentType.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("employmentType",
                "Invalid employment type: " + type + ". Must be: permanent or contract");
        }
    }

    private StaffDto toDto(StaffMember staff) {
        List<StaffRole> staffRoles = staffRoleRepository.findByStaff_IdAndActive(staff.getId(), true);
        List<StaffSite> staffSites = staffSiteRepository.findByStaff_IdAndActive(staff.getId(), true);
        List<StaffCompetenceLevel> competenceLevels = competenceLevelRepository.findByStaff_Id(staff.getId());

        // Load work parameters from StaffWorkParameters (single source of truth)
        Optional<StaffWorkParameters> workParamsOpt = staffWorkParametersRepository.findCurrentByStaffId(staff.getId());
        StaffWorkParameters workParams = workParamsOpt.orElse(null);

        // Convert to summary DTOs with id, name, and skill level
        List<RoleSummaryDto> roles = staffRoles.stream()
            .map(sr -> RoleSummaryDto.withSkillLevel(
                sr.getRole().getId(),
                sr.getRole().getName(),
                sr.getSkillLevel().name(),
                sr.isPrimary()))
            .toList();

        List<SiteSummaryDto> sites = staffSites.stream()
            .map(ss -> new SiteSummaryDto(ss.getSite().getId(), ss.getSite().getName(), ss.isPrimary()))
            .toList();

        return new StaffDto(
            staff.getId(),
            staff.getEmployeeCode(),
            staff.getFirstName(),
            staff.getLastName(),
            staff.getEmail(),
            staff.getPhone(),
            staff.getSecondaryPhone(),
            staff.getEmploymentStatus().name(),
            staff.getEmploymentType().name(),
            staff.getHireDate(),
            staff.getTerminationDate(),
            roles,
            sites,
            competenceLevels.stream().map(StaffCompetenceLevel::getLevel).sorted().toList(),
            staff.getVersion(),
            workParams != null ? workParams.getMinHoursPerDay() : null,
            workParams != null ? workParams.getMaxHoursPerDay() : null,
            workParams != null ? workParams.getMinHoursPerWeek() : null,
            workParams != null ? workParams.getMaxHoursPerWeek() : null,
            workParams != null ? workParams.getMinHoursPerMonth() : null,
            workParams != null ? workParams.getMaxHoursPerMonth() : null,
            workParams != null ? workParams.getMinDaysOffPerWeek() : null,
            workParams != null ? workParams.getMaxSitesPerDay() : null,
            // Mandatory leave enforcement fields
            staff.getMustGoOnLeaveAfterDays(),
            staff.getAccruesOneDayLeaveAfterDays(),
            staff.getLastWorkedDate(),
            staff.getConsecutiveWorkDays(),
            staff.getAccruedMandatoryLeaveDays(),
            computeAllocationReadiness(staff)
        );
    }

    private String computeAllocationReadiness(StaffMember staff) {
        List<StaffRole> activeRoles = staffRoleRepository.findByStaff_IdAndActive(staff.getId(), true);
        if (activeRoles.isEmpty()) {
            return "NO_ASSIGNED_ROLE";
        }

        List<StaffSite> activeSites = staffSiteRepository.findByStaff_IdAndActive(staff.getId(), true);
        if (activeSites.isEmpty()) {
            return "NO_ASSIGNED_SITE";
        }

        Optional<StaffWorkParameters> workParams = staffWorkParametersRepository.findCurrentByStaffId(staff.getId());
        if (workParams.isEmpty()) {
            return "MISSING_WORK_PARAMETERS";
        }

        boolean hasCompensation = activeRoles.stream()
            .anyMatch(sr -> staffCompensationRepository
                .findActiveByStaffIdAndRoleIdAndDate(staff.getId(), sr.getRole().getId(), LocalDate.now())
                .isPresent());
        if (!hasCompensation) {
            return "MISSING_COMPENSATION";
        }

        return "READY";
    }
}
