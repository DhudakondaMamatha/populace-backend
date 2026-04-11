package com.populace.staff.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.Role;
import com.populace.domain.StaffMember;
import com.populace.domain.StaffRole;
import com.populace.repository.RoleRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.StaffRoleRepository;
import com.populace.staff.dto.StaffRoleDto;
import com.populace.staff.dto.StaffRoleUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StaffRoleService {

    private static final Set<String> VALID_COMPETENCE_LEVELS = Set.of("L1", "L2", "L3");

    private final StaffRoleRepository staffRoleRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final RoleRepository roleRepository;

    public StaffRoleService(StaffRoleRepository staffRoleRepository,
                            StaffMemberRepository staffMemberRepository,
                            RoleRepository roleRepository) {
        this.staffRoleRepository = staffRoleRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffRoleDto> getStaffRoles(Long businessId, Long staffId) {
        StaffMember staff = getStaffOrThrow(businessId, staffId);
        return staffRoleRepository.findByStaffIdAndActiveWithRole(staffId, true)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StaffRoleDto> getPrimaryRole(Long businessId, Long staffId) {
        getStaffOrThrow(businessId, staffId);
        return staffRoleRepository.findPrimaryRoleByStaffId(staffId)
            .map(this::toDto);
    }

    @Transactional
    public StaffRoleDto setPrimaryRole(Long businessId, Long staffId, Long roleId) {
        StaffMember staff = getStaffOrThrow(businessId, staffId);

        StaffRole staffRole = staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId)
            .orElseThrow(() -> new ValidationException(
                "Staff member is not assigned to role with id " + roleId));

        if (!staffRole.isActive()) {
            throw new ValidationException("Cannot set inactive role as primary");
        }

        // Toggle primary flag (multiple roles can be primary)
        staffRole.setPrimary(!staffRole.isPrimary());
        staffRoleRepository.save(staffRole);

        return toDto(staffRole);
    }

    @Transactional
    public StaffRoleDto updateStaffRole(Long businessId, Long staffId, Long roleId,
                                        StaffRoleUpdateRequest request) {
        getStaffOrThrow(businessId, staffId);

        StaffRole staffRole = staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId)
            .orElseThrow(() -> new ValidationException(
                "Staff member is not assigned to role with id " + roleId));

        validateStaffRoleUpdate(request);

        // Update competence level if provided
        if (request.competenceLevel() != null) {
            staffRole.setCompetenceLevel(request.competenceLevel());
        }

        // Update break override fields
        staffRole.setMinBreakMinutes(request.minBreakMinutes());
        staffRole.setMaxBreakMinutes(request.maxBreakMinutes());
        staffRole.setMaxBreakDurationMinutes(request.maxBreakDurationMinutes());
        staffRole.setMinWorkMinutesBeforeBreak(request.minWorkMinutesBeforeBreak());
        staffRole.setMaxContinuousWorkMinutes(request.maxContinuousWorkMinutes());
        staffRoleRepository.save(staffRole);

        return toDto(staffRole);
    }

    /**
     * @deprecated Use updateStaffRole instead
     */
    @Deprecated
    @Transactional
    public StaffRoleDto updateBreakOverride(Long businessId, Long staffId, Long roleId,
                                            StaffRoleUpdateRequest request) {
        return updateStaffRole(businessId, staffId, roleId, request);
    }

    @Transactional
    public void clearBreakOverride(Long businessId, Long staffId, Long roleId) {
        getStaffOrThrow(businessId, staffId);

        StaffRole staffRole = staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId)
            .orElseThrow(() -> new ValidationException(
                "Staff member is not assigned to role with id " + roleId));

        staffRole.setMinBreakMinutes(null);
        staffRole.setMaxBreakMinutes(null);
        staffRole.setMaxBreakDurationMinutes(null);
        staffRole.setMinWorkMinutesBeforeBreak(null);
        staffRole.setMaxContinuousWorkMinutes(null);
        staffRoleRepository.save(staffRole);
    }

    @Transactional
    public StaffRoleDto assignRole(Long businessId, Long staffId, Long roleId, boolean isPrimary) {
        StaffMember staff = getStaffOrThrow(businessId, staffId);
        Role role = roleRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(roleId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        // Check if already assigned
        Optional<StaffRole> existing = staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId);
        if (existing.isPresent()) {
            StaffRole staffRole = existing.get();
            if (!staffRole.isActive()) {
                staffRole.setActive(true);
            }
            staffRole.setPrimary(isPrimary);
            staffRoleRepository.save(staffRole);
            return toDto(staffRole);
        }

        // Create new assignment
        StaffRole staffRole = new StaffRole();
        staffRole.setStaff(staff);
        staffRole.setRole(role);
        staffRole.setPrimary(isPrimary);
        staffRole.setActive(true);

        staffRoleRepository.save(staffRole);
        return toDto(staffRole);
    }

    private void validateStaffRoleUpdate(StaffRoleUpdateRequest request) {
        // Validate competence level
        String competenceLevel = request.competenceLevel();
        if (competenceLevel != null && !VALID_COMPETENCE_LEVELS.contains(competenceLevel)) {
            throw new ValidationException(
                "Invalid competence level: " + competenceLevel + ". Valid levels: L1, L2, L3");
        }

        // Validate break overrides
        Integer minMinutes = request.minBreakMinutes();
        Integer maxMinutes = request.maxBreakMinutes();
        Integer maxDurationMinutes = request.maxBreakDurationMinutes();

        if (minMinutes != null && maxMinutes != null) {
            if (maxMinutes < minMinutes) {
                throw new ValidationException(
                    "Max break minutes must be greater than or equal to min break minutes");
            }
        }

        if (minMinutes != null && minMinutes <= 0) {
            throw new ValidationException("Min break minutes must be positive");
        }
        if (maxMinutes != null && maxMinutes <= 0) {
            throw new ValidationException("Max break minutes must be positive");
        }

        // Max break duration validation
        if (maxDurationMinutes != null && maxDurationMinutes <= 0) {
            throw new ValidationException("Max break duration minutes must be positive");
        }
        if (maxDurationMinutes != null && minMinutes != null) {
            if (maxDurationMinutes < minMinutes) {
                throw new ValidationException(
                    "Max break duration minutes must be greater than or equal to min break minutes");
            }
        }

        if (request.minWorkMinutesBeforeBreak() != null) {
            if (request.minWorkMinutesBeforeBreak() < 30 || request.minWorkMinutesBeforeBreak() > 480) {
                throw new ValidationException("minWorkMinutesBeforeBreak", "Minimum work before break must be between 30 and 480 minutes");
            }
        }
        if (request.maxContinuousWorkMinutes() != null) {
            if (request.maxContinuousWorkMinutes() < 60 || request.maxContinuousWorkMinutes() > 480) {
                throw new ValidationException("maxContinuousWorkMinutes", "Maximum continuous work must be between 60 and 480 minutes");
            }
        }
    }

    private StaffMember getStaffOrThrow(Long businessId, Long staffId) {
        return staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
    }

    private StaffRoleDto toDto(StaffRole staffRole) {
        Role role = staffRole.getRole();
        String skillLevel = staffRole.getSkillLevel().name();

        return StaffRoleDto.withSkillLevel(
            staffRole.getId(),
            role.getId(),
            role.getName(),
            skillLevel,
            staffRole.isPrimary(),
            staffRole.getMinBreakMinutes(),
            staffRole.getMaxBreakMinutes(),
            staffRole.getMaxBreakDurationMinutes(),
            staffRole.getEffectiveMinBreakMinutes(),
            staffRole.getEffectiveMaxBreakMinutes(),
            staffRole.getEffectiveMaxBreakDurationMinutes(),
            staffRole.getMinWorkMinutesBeforeBreak(),
            staffRole.getMaxContinuousWorkMinutes(),
            staffRole.getEffectiveMinWorkMinutesBeforeBreak(),
            staffRole.getEffectiveMaxContinuousWorkMinutes(),
            staffRole.hasBreakOverride(),
            staffRole.isActive()
        );
    }
}
