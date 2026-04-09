package com.populace.role.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.Business;
import com.populace.domain.Role;
import com.populace.repository.BusinessRepository;
import com.populace.repository.RoleRepository;
import com.populace.role.dto.RoleCreateRequest;
import com.populace.role.dto.RoleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Role Service - Simplified to handle metadata only.
 * Constraint fields have been moved to Staff (personal) and Business (system).
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final BusinessRepository businessRepository;

    public RoleService(RoleRepository roleRepository,
                       BusinessRepository businessRepository) {
        this.roleRepository = roleRepository;
        this.businessRepository = businessRepository;
    }

    public List<RoleDto> listRoles(Long businessId, String status, String search) {
        return roleRepository.findByBusiness_IdAndDeletedAtIsNull(businessId)
            .stream()
            .filter(role -> matchesFilters(role, status, search))
            .map(this::toDto)
            .toList();
    }

    public RoleDto getRoleById(Long businessId, Long roleId) {
        Role role = getRoleOrThrow(businessId, roleId);
        return toDto(role);
    }

    @Transactional
    public RoleDto createRole(Long businessId, RoleCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        Role role = new Role();
        role.setBusiness(business);
        role.setActive(true);
        applyRequestToRole(role, request);

        role = roleRepository.save(role);
        return toDto(role);
    }

    @Transactional
    public RoleDto updateRole(Long businessId, Long roleId, RoleCreateRequest request) {
        Role role = getRoleOrThrow(businessId, roleId);
        applyRequestToRole(role, request);

        role = roleRepository.save(role);
        return toDto(role);
    }

    @Transactional
    public void deactivateRole(Long businessId, Long roleId) {
        Role role = getRoleOrThrow(businessId, roleId);
        role.setActive(false);
        roleRepository.save(role);
    }

    private Role getRoleOrThrow(Long businessId, Long roleId) {
        return roleRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(roleId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    private boolean matchesFilters(Role role, String status, String search) {
        if (status != null && !status.isEmpty()) {
            boolean isActive = "active".equalsIgnoreCase(status);
            if (role.isActive() != isActive) {
                return false;
            }
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            boolean nameMatches = role.getName() != null &&
                role.getName().toLowerCase().contains(searchLower);
            boolean descMatches = role.getDescription() != null &&
                role.getDescription().toLowerCase().contains(searchLower);
            return nameMatches || descMatches;
        }

        return true;
    }

    private void applyRequestToRole(Role role, RoleCreateRequest request) {
        role.setName(request.name());
        role.setCode(request.code());
        role.setDescription(request.description());
        role.setColor(request.color());

        if (request.defaultRole() != null) {
            role.setDefaultRole(request.defaultRole());
        }
    }

    private RoleDto toDto(Role role) {
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getCode(),
            role.getDescription(),
            role.getColor(),
            role.isDefaultRole(),
            role.isActive()
        );
    }
}
