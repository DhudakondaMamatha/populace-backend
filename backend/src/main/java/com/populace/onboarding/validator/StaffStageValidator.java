package com.populace.onboarding.validator;

import com.populace.onboarding.dto.StageValidationResult;
import com.populace.repository.StaffMemberRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("staffStageValidator")
public class StaffStageValidator implements StageValidator {

    private final StaffMemberRepository staffMemberRepository;

    public StaffStageValidator(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    @Override
    public boolean isComplete(Long businessId) {
        return staffMemberRepository.existsByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    @Override
    public StageValidationResult validate(Long businessId) {
        long staffCount = staffMemberRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
        boolean hasStaff = staffCount > 0;

        String message = hasStaff
            ? "At least 1 staff member exists"
            : "No staff members added yet";

        return StageValidationResult.withDetails(
            hasStaff,
            message,
            Map.of("staffCount", staffCount)
        );
    }
}
