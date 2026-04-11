package com.populace.onboarding.validator;

import com.populace.onboarding.dto.StageValidationResult;

/**
 * Interface for validating onboarding stage completion.
 * Each stage has its own validator implementation.
 */
public interface StageValidator {

    /**
     * Quick check if this stage is complete for the given business.
     *
     * @param businessId the business to check
     * @return true if the stage requirements are met
     */
    boolean isComplete(Long businessId);

    /**
     * Detailed validation with result message.
     *
     * @param businessId the business to validate
     * @return validation result with completion status and message
     */
    StageValidationResult validate(Long businessId);
}
