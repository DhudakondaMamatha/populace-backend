package com.populace.onboarding.validator;

import com.populace.onboarding.dto.StageValidationResult;
import com.populace.repository.SiteRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("siteStageValidator")
public class SiteStageValidator implements StageValidator {

    private final SiteRepository siteRepository;

    public SiteStageValidator(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public boolean isComplete(Long businessId) {
        return siteRepository.existsByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    @Override
    public StageValidationResult validate(Long businessId) {
        long siteCount = siteRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
        boolean hasSites = siteCount > 0;

        String message = hasSites
            ? "At least 1 site exists"
            : "No sites created yet";

        return StageValidationResult.withDetails(
            hasSites,
            message,
            Map.of("siteCount", siteCount)
        );
    }
}
