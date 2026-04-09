package com.populace.business.service;

import com.populace.business.dto.BusinessSettingsDto;
import com.populace.business.dto.BusinessSettingsUpdateRequest;
import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.Business;
import com.populace.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessSettingsService {

    private final BusinessRepository businessRepository;

    public BusinessSettingsService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsDto getSettings(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
        return toDto(business);
    }

    @Transactional
    public BusinessSettingsDto updateSettings(Long businessId, BusinessSettingsUpdateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        if (request.coverageWindowMinutes() != null) {
            business.setCoverageWindowMinutes(request.coverageWindowMinutes());
        }
        if (request.toleranceOverPercentage() != null) {
            business.setToleranceOverPercentage(request.toleranceOverPercentage());
        }
        if (request.toleranceUnderPercentage() != null) {
            business.setToleranceUnderPercentage(request.toleranceUnderPercentage());
        }

        businessRepository.save(business);
        return toDto(business);
    }

    private BusinessSettingsDto toDto(Business business) {
        return new BusinessSettingsDto(
            business.getId(),
            business.getName(),
            business.getCoverageWindowMinutes(),
            business.getToleranceOverPercentage(),
            business.getToleranceUnderPercentage()
        );
    }
}
