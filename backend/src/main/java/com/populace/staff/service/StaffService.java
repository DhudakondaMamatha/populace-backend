package com.populace.staff.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.*;
import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.EmploymentType;
import com.populace.repository.*;
import com.populace.staff.dto.RoleSummaryDto;
import com.populace.staff.dto.SiteSummaryDto;
import com.populace.staff.dto.StaffCreateRequest;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.StaffUpdateRequest;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StaffService {

    private static final Set<String> VALID_COMPETENCE_LEVELS = Set.of("L1", "L2", "L3");

    private final StaffMemberRepository staffRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final StaffSiteRepository staffSiteRepository;
    private final StaffCompetenceLevelRepository competenceLevelRepository;
    private final StaffCompensationRepository staffCompensationRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final BusinessRepository businessRepository;
    private final UnifiedStaffProvisioningService provisioningService;
    private final StaffConstraintValidator constraintValidator;
    private final StaffWorkParametersRepository staffWorkParametersRepository;

    public StaffService(StaffMemberRepository staffRepository,
                        StaffRoleRepository staffRoleRepository,
                        StaffSiteRepository staffSiteRepository,
                        StaffCompetenceLevelRepository competenceLevelRepository,
                        StaffCompensationRepository staffCompensationRepository,
                        RoleRepository roleRepository,
                        SiteRepository siteRepository,
                        BusinessRepository businessRepository,
                        UnifiedStaffProvisioningService provisioningService,
                        StaffConstraintValidator constraintValidator,
                        StaffWorkParametersRepository staffWorkParametersRepository) {
        this.staffRepository = staffRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.staffSiteRepository = staffSiteRepository;
        this.competenceLevelRepository = competenceLevelRepository;
        this.staffCompensationRepository = staffCompensationRepository;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
        this.businessRepository = businessRepository;
        this.provisioningService = provisioningService;
        this.constraintValidator = constraintValidator;
        this.staffWorkParametersRepository = staffWorkParametersRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffDto> listStaff(Long businessId, String status, String employmentType,
                                     Long roleId, Long siteId, String search) {
        return staffRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).stream()
            .filter(staff -> {
                // Status filter
                if (status != null && !status.isEmpty()) {
                    if (!staff.getEmploymentStatus().name().equalsIgnoreCase(status)) {
                        return false;
                    }
                }
                // Employment type filter
                if (employmentType != null && !employmentType.isEmpty()) {
                    if (!staff.getEmploymentType().name().equalsIgnoreCase(employmentType)) {
                        return false;
                    }
                }
                // Role filter
                if (roleId != null) {
                    List<StaffRole> roles = staffRoleRepository.findByStaff_IdAndActive(staff.getId(), true);
                    boolean hasRole = roles.stream().anyMatch(sr -> sr.getRole().getId().equals(roleId));
                    if (!hasRole) return false;
                }
                // Site filter
                if (siteId != null) {
                    List<StaffSite> sites = staffSiteRepository.findByStaff_IdAndActive(staff.getId(), true);
                    boolean hasSite = sites.stream().anyMatch(ss -> ss.getSite().getId().equals(siteId));
                    if (!hasSite) return false;
                }
                // Search filter (name, email, employee code)
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    String fullName = (staff.getFirstName() + " " + staff.getLastName()).toLowerCase();
                    boolean matches = fullName.contains(searchLower)
                        || (staff.getEmail() != null && staff.getEmail().toLowerCase().contains(searchLower))
                        || (staff.getEmployeeCode() != null && staff.getEmployeeCode().toLowerCase().contains(searchLower));
                    if (!matches) return false;
                }
                return true;
            })
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public StaffDto getStaffById(Long businessId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
        return toDto(staff);
    }

    /**
     * Creates a fully provisioned staff member using the unified creation model.
     * This is the recommended method for creating new staff.
     *
     * @param businessId The business ID
     * @param request The unified creation request with all staff data
     * @return The created staff DTO
     */
    @Transactional
    public StaffDto createStaffWithFullProvisioning(Long businessId, UnifiedStaffCreateRequest request) {
        return provisioningService.createStaffWithFullProvisioning(businessId, request);
    }

    /**
     * @deprecated Use {@link #createStaffWithFullProvisioning(Long, UnifiedStaffCreateRequest)} instead.
     * This method creates staff with only basic fields for backward compatibility.
     */
    @Deprecated
    @Transactional
    public StaffDto createStaff(Long businessId, StaffCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        StaffMember staff = new StaffMember();
        staff.setBusiness(business);
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setEmail(request.email());
        staff.setPhone(request.phone());
        staff.setEmployeeCode(request.employeeCode());
        staff.setEmploymentType(parseEmploymentType(request.employmentType()));
        staff.setEmploymentStatus(EmploymentStatus.active);

        staff = staffRepository.save(staff);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            boolean isFirst = true;
            for (Long roleId : request.roleIds()) {
                createStaffRole(staff, roleId, isFirst);
                isFirst = false;
            }
        }

        if (request.siteIds() != null) {
            for (Long siteId : request.siteIds()) {
                createStaffSite(staff, siteId);
            }
        }

        return toDto(staff);
    }

    private void createStaffRole(StaffMember staff, Long roleId, boolean isPrimary) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) return;

        StaffRole staffRole = new StaffRole();
        staffRole.setStaff(staff);
        staffRole.setRole(role);
        staffRole.setPrimary(isPrimary);
        staffRole.setActive(true);

        staffRoleRepository.save(staffRole);
    }

    private void createStaffSite(StaffMember staff, Long siteId) {
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) return;

        StaffSite staffSite = new StaffSite();
        staffSite.setStaff(staff);
        staffSite.setSite(site);
        staffSite.setActive(true);
        staffSiteRepository.save(staffSite);
    }

    @Transactional
    public StaffDto updateStaff(Long businessId, Long staffId, StaffUpdateRequest request) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        // Load current work parameters for validation and update
        final StaffMember staffRef = staff;
        StaffWorkParameters workParams = staffWorkParametersRepository.findCurrentByStaffId(staffId)
            .orElseGet(() -> {
                StaffWorkParameters newParams = new StaffWorkParameters();
                newParams.setStaff(staffRef);
                newParams.setEffectiveFrom(LocalDate.now());
                return newParams;
            });

        // Validate constraints if any constraint field is being updated
        boolean hasConstraintUpdate = request.minHoursPerDay() != null
            || request.maxHoursPerDay() != null
            || request.maxHoursPerMonth() != null
            || request.minDaysOffPerWeek() != null
            || request.maxSitesPerDay() != null
            || request.minHoursPerWeek() != null
            || request.maxHoursPerWeek() != null;

        if (hasConstraintUpdate) {
            constraintValidator.ensureConstraintsAreWithinLimitsForUpdate(
                businessId,
                request.minHoursPerDay(),
                request.maxHoursPerDay(),
                request.minHoursPerMonth(),
                request.maxHoursPerMonth(),
                request.minDaysOffPerWeek(),
                request.maxSitesPerDay(),
                request.minHoursPerWeek(),
                request.maxHoursPerWeek(),
                workParams.getMinHoursPerDay(),
                workParams.getMaxHoursPerDay(),
                workParams.getMinHoursPerMonth(),
                workParams.getMaxHoursPerMonth(),
                workParams.getMinDaysOffPerWeek(),
                workParams.getMaxSitesPerDay(),
                workParams.getMinHoursPerWeek(),
                workParams.getMaxHoursPerWeek()
            );
        }

        // Only update non-null basic fields
        if (request.firstName() != null) staff.setFirstName(request.firstName());
        if (request.lastName() != null) staff.setLastName(request.lastName());
        if (request.email() != null) staff.setEmail(request.email());
        if (request.phone() != null) staff.setPhone(request.phone());
        if (request.employeeCode() != null) staff.setEmployeeCode(request.employeeCode());
        if (request.employmentType() != null) staff.setEmploymentType(parseEmploymentType(request.employmentType()));

        // Update work parameters in StaffWorkParameters (single source of truth)
        if (request.minHoursPerDay() != null) {
            workParams.setMinHoursPerDay(request.minHoursPerDay());
        }
        if (request.maxHoursPerDay() != null) {
            workParams.setMaxHoursPerDay(request.maxHoursPerDay());
        }
        if (request.minHoursPerMonth() != null) {
            workParams.setMinHoursPerMonth(request.minHoursPerMonth());
        }
        if (request.maxHoursPerMonth() != null) {
            workParams.setMaxHoursPerMonth(request.maxHoursPerMonth());
        }
        if (request.minDaysOffPerWeek() != null) {
            workParams.setMinDaysOffPerWeek(request.minDaysOffPerWeek());
        }
        if (request.maxSitesPerDay() != null) {
            workParams.setMaxSitesPerDay(request.maxSitesPerDay());
        }
        if (request.minHoursPerWeek() != null) {
            workParams.setMinHoursPerWeek(request.minHoursPerWeek());
        }
        if (request.maxHoursPerWeek() != null) {
            workParams.setMaxHoursPerWeek(request.maxHoursPerWeek());
        }

        if (hasConstraintUpdate) {
            staffWorkParametersRepository.save(workParams);
        }

        // Update mandatory leave enforcement fields
        if (request.mustGoOnLeaveAfterDays() != null) {
            staff.setMustGoOnLeaveAfterDays(request.mustGoOnLeaveAfterDays());
        }
        if (request.accruesOneDayLeaveAfterDays() != null) {
            staff.setAccruesOneDayLeaveAfterDays(request.accruesOneDayLeaveAfterDays());
        }

        staff = staffRepository.save(staff);
        return toDto(staff);
    }

    @Transactional
    public void terminateStaff(Long businessId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        staff.setEmploymentStatus(EmploymentStatus.terminated);
        staff.setDeletedAt(Instant.now());
        staffRepository.save(staff);
    }

    @Transactional
    public StaffDto assignRoles(Long businessId, Long staffId, List<Long> roleIds, List<Long> primaryIds) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        List<Long> effectivePrimaryIds = primaryIds != null ? primaryIds : List.of();

        deactivateAllRolesForStaff(staffId);

        for (Long roleId : roleIds) {
            boolean isPrimary = effectivePrimaryIds.contains(roleId);
            assignOrReactivateRole(staff, roleId, isPrimary);
        }

        return toDto(staff);
    }

    private void deactivateAllRolesForStaff(Long staffId) {
        List<StaffRole> activeRoles = staffRoleRepository.findByStaff_IdAndActive(staffId, true);
        for (StaffRole staffRole : activeRoles) {
            staffRole.setActive(false);
            staffRoleRepository.save(staffRole);
        }
    }

    private void assignOrReactivateRole(StaffMember staff, Long roleId, boolean isPrimary) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            return;
        }

        StaffRole existingStaffRole = staffRoleRepository
            .findByStaff_IdAndRole_Id(staff.getId(), roleId)
            .orElse(null);

        if (existingStaffRole != null) {
            existingStaffRole.setActive(true);
            existingStaffRole.setPrimary(isPrimary);
            staffRoleRepository.save(existingStaffRole);
        } else {
            StaffRole newStaffRole = new StaffRole();
            newStaffRole.setStaff(staff);
            newStaffRole.setRole(role);
            newStaffRole.setPrimary(isPrimary);
            newStaffRole.setActive(true);
            staffRoleRepository.save(newStaffRole);
        }
    }

    @Transactional
    public StaffDto assignSites(Long businessId, Long staffId, List<Long> siteIds) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        deactivateAllSitesForStaff(staffId);

        for (Long siteId : siteIds) {
            assignOrReactivateSite(staff, siteId);
        }

        return toDto(staff);
    }

    private void deactivateAllSitesForStaff(Long staffId) {
        List<StaffSite> activeSites = staffSiteRepository.findByStaff_IdAndActive(staffId, true);
        for (StaffSite staffSite : activeSites) {
            staffSite.setActive(false);
            staffSiteRepository.save(staffSite);
        }
    }

    private void assignOrReactivateSite(StaffMember staff, Long siteId) {
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            return;
        }

        StaffSite existingStaffSite = staffSiteRepository
            .findByStaff_IdAndSite_Id(staff.getId(), siteId)
            .orElse(null);

        if (existingStaffSite != null) {
            existingStaffSite.setActive(true);
            staffSiteRepository.save(existingStaffSite);
        } else {
            StaffSite newStaffSite = new StaffSite();
            newStaffSite.setStaff(staff);
            newStaffSite.setSite(site);
            newStaffSite.setActive(true);
            staffSiteRepository.save(newStaffSite);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getCompetenceLevels(Long businessId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        return competenceLevelRepository.findByStaff_Id(staffId).stream()
            .map(StaffCompetenceLevel::getLevel)
            .sorted()
            .toList();
    }

    @Transactional
    public List<String> updateCompetenceLevels(Long businessId, Long staffId, List<String> levels) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        // Validate levels
        for (String level : levels) {
            if (!VALID_COMPETENCE_LEVELS.contains(level)) {
                throw new ValidationException("competenceLevels",
                    "Invalid competence level: " + level + ". Valid levels are: L1, L2, L3");
            }
        }

        // Remove duplicates
        Set<String> uniqueLevels = Set.copyOf(levels);

        // Delete existing levels
        competenceLevelRepository.deleteAllByStaffId(staffId);

        // Add new levels
        for (String level : uniqueLevels) {
            StaffCompetenceLevel competenceLevel = new StaffCompetenceLevel(staff, level);
            competenceLevelRepository.save(competenceLevel);
        }

        return uniqueLevels.stream().sorted().toList();
    }

    private EmploymentType parseEmploymentType(String type) {
        if (type == null) return EmploymentType.permanent;
        try {
            return EmploymentType.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            return EmploymentType.permanent;
        }
    }

    private StaffDto toDto(StaffMember staff) {
        List<StaffRole> staffRoles = staffRoleRepository.findByStaff_IdAndActive(staff.getId(), true);
        List<StaffSite> staffSites = staffSiteRepository.findByStaff_IdAndActive(staff.getId(), true);

        // Fetch competence levels with fallback for when table doesn't exist yet
        List<String> competenceLevelNames;
        try {
            List<StaffCompetenceLevel> competenceLevels = competenceLevelRepository.findByStaff_Id(staff.getId());
            competenceLevelNames = competenceLevels.stream()
                .map(StaffCompetenceLevel::getLevel)
                .sorted()
                .toList();
        } catch (Exception e) {
            // Table may not exist yet - return empty list
            competenceLevelNames = List.of();
        }

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
            competenceLevelNames,
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
