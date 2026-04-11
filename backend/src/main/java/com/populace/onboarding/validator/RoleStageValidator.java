package com.populace.onboarding.validator;

import com.populace.onboarding.dto.StageValidationResult;
import com.populace.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("roleStageValidator")
public class RoleStageValidator implements StageValidator {

    private final RoleRepository roleRepository;

    public RoleStageValidator(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public boolean isComplete(Long businessId) {
        return roleRepository.existsByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    @Override
    public StageValidationResult validate(Long businessId) {
        long roleCount = roleRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
        boolean hasRoles = roleCount > 0;

        String message = hasRoles
            ? "At least 1 role exists"
            : "No roles created yet";

        return StageValidationResult.withDetails(
            hasRoles,
            message,
            Map.of("roleCount", roleCount)
        );
    }
}
