package com.populace.service;

import com.populace.domain.BusinessConfiguration;
import com.populace.repository.BusinessConfigurationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for accessing business configuration settings.
 */
@Service
public class BusinessConfigurationService {

    private static final BigDecimal DEFAULT_MONTHLY_TOLERANCE = new BigDecimal("10.00");

    private final BusinessConfigurationRepository repository;

    public BusinessConfigurationService(BusinessConfigurationRepository repository) {
        this.repository = repository;
    }

    /**
     * Get the monthly hour tolerance percent for a business.
     * Returns the configured value or 10% default if not configured.
     */
    public BigDecimal getMonthlyTolerancePercent(Long businessId) {
        return repository.findByBusiness_Id(businessId)
            .map(BusinessConfiguration::getMonthlyHourTolerancePercent)
            .orElse(DEFAULT_MONTHLY_TOLERANCE);
    }
}
