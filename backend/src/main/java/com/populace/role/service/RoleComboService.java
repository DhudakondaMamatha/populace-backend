package com.populace.role.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.Business;
import com.populace.domain.Role;
import com.populace.domain.RoleCombo;
import com.populace.domain.RoleComboRole;
import com.populace.repository.BusinessRepository;
import com.populace.repository.RoleComboRepository;
import com.populace.repository.RoleComboRoleRepository;
import com.populace.repository.RoleRepository;
import com.populace.role.dto.RoleComboCreateRequest;
import com.populace.role.dto.RoleComboDto;
import com.populace.role.dto.RoleComboRoleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleComboService {

    private final RoleComboRepository roleComboRepository;
    private final RoleComboRoleRepository roleComboRoleRepository;
    private final RoleRepository roleRepository;
    private final BusinessRepository businessRepository;

    public RoleComboService(RoleComboRepository roleComboRepository,
                            RoleComboRoleRepository roleComboRoleRepository,
                            RoleRepository roleRepository,
                            BusinessRepository businessRepository) {
        this.roleComboRepository = roleComboRepository;
        this.roleComboRoleRepository = roleComboRoleRepository;
        this.roleRepository = roleRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleComboDto> listCombos(Long businessId) {
        return roleComboRepository.findByBusiness_Id(businessId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public RoleComboDto createCombo(Long businessId, RoleComboCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        RoleCombo combo = new RoleCombo();
        combo.setBusiness(business);
        combo.setName(request.name());
        combo.setColor(request.color());
        combo.setActive(true);
        combo = roleComboRepository.save(combo);

        saveRoleLinks(combo, request.roleIds());
        return toDto(combo);
    }

    @Transactional
    public RoleComboDto updateCombo(Long businessId, Long comboId, RoleComboCreateRequest request) {
        RoleCombo combo = getComboOrThrow(businessId, comboId);
        combo.setName(request.name());
        combo.setColor(request.color());
        combo = roleComboRepository.save(combo);

        roleComboRoleRepository.deleteByRoleCombo_Id(comboId);
        saveRoleLinks(combo, request.roleIds());
        return toDto(combo);
    }

    @Transactional
    public void deactivateCombo(Long businessId, Long comboId) {
        RoleCombo combo = getComboOrThrow(businessId, comboId);
        combo.setActive(false);
        roleComboRepository.save(combo);
    }

    private RoleCombo getComboOrThrow(Long businessId, Long comboId) {
        return roleComboRepository.findByIdAndBusiness_Id(comboId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("RoleCombo", comboId));
    }

    private void saveRoleLinks(RoleCombo combo, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
            RoleComboRole link = new RoleComboRole();
            link.setRoleCombo(combo);
            link.setRole(role);
            roleComboRoleRepository.save(link);
        }
    }

    private RoleComboDto toDto(RoleCombo combo) {
        List<RoleComboRoleDto> roles = roleComboRoleRepository.findByRoleCombo_Id(combo.getId())
            .stream()
            .map(rcr -> new RoleComboRoleDto(
                rcr.getRole().getId(),
                rcr.getRole().getName(),
                rcr.getRole().getColor()
            ))
            .toList();

        return new RoleComboDto(
            combo.getId(),
            combo.getName(),
            combo.getColor(),
            combo.isActive(),
            roles
        );
    }
}
