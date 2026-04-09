package com.populace.site.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.Site;
import com.populace.domain.SiteOperatingHours;
import com.populace.repository.SiteOperatingHoursRepository;
import com.populace.repository.SiteRepository;
import com.populace.site.dto.BulkOperatingHoursRequest;
import com.populace.site.dto.OperatingHoursDto;
import com.populace.site.dto.OperatingHoursRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SiteOperatingHoursService {

    private final SiteOperatingHoursRepository operatingHoursRepository;
    private final SiteRepository siteRepository;

    public SiteOperatingHoursService(SiteOperatingHoursRepository operatingHoursRepository,
                                     SiteRepository siteRepository) {
        this.operatingHoursRepository = operatingHoursRepository;
        this.siteRepository = siteRepository;
    }

    public List<OperatingHoursDto> getOperatingHours(Long businessId, Long siteId) {
        validateSiteAccess(businessId, siteId);
        return operatingHoursRepository.findBySite_IdOrderByDayOfWeek(siteId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public OperatingHoursDto setOperatingHours(Long businessId, Long siteId, OperatingHoursRequest request) {
        Site site = validateSiteAccess(businessId, siteId);
        validateRequest(request);

        SiteOperatingHours hours = operatingHoursRepository
            .findBySite_IdAndDayOfWeek(siteId, request.dayOfWeek())
            .orElseGet(() -> {
                SiteOperatingHours newHours = new SiteOperatingHours();
                newHours.setSite(site);
                newHours.setDayOfWeek(request.dayOfWeek());
                return newHours;
            });

        hours.setClosed(request.isClosed());
        hours.setOpenTime(request.isClosed() ? null : request.openTime());
        hours.setCloseTime(request.isClosed() ? null : request.closeTime());

        hours = operatingHoursRepository.save(hours);
        return toDto(hours);
    }

    @Transactional
    public List<OperatingHoursDto> setBulkOperatingHours(Long businessId, Long siteId, BulkOperatingHoursRequest request) {
        Site site = validateSiteAccess(businessId, siteId);

        // Validate all requests first
        for (OperatingHoursRequest hourRequest : request.hours()) {
            validateRequest(hourRequest);
        }

        // Delete existing hours and replace with new ones
        operatingHoursRepository.deleteBySite_Id(siteId);

        List<SiteOperatingHours> hoursList = request.hours().stream()
            .map(hourRequest -> {
                SiteOperatingHours hours = new SiteOperatingHours();
                hours.setSite(site);
                hours.setDayOfWeek(hourRequest.dayOfWeek());
                hours.setClosed(hourRequest.isClosed());
                hours.setOpenTime(hourRequest.isClosed() ? null : hourRequest.openTime());
                hours.setCloseTime(hourRequest.isClosed() ? null : hourRequest.closeTime());
                return hours;
            })
            .toList();

        return operatingHoursRepository.saveAll(hoursList).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public void deleteOperatingHours(Long businessId, Long siteId, Integer dayOfWeek) {
        validateSiteAccess(businessId, siteId);
        SiteOperatingHours hours = operatingHoursRepository
            .findBySite_IdAndDayOfWeek(siteId, dayOfWeek)
            .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found for day " + dayOfWeek));
        operatingHoursRepository.delete(hours);
    }

    private Site validateSiteAccess(Long businessId, Long siteId) {
        return siteRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(siteId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Site", siteId));
    }

    private void validateRequest(OperatingHoursRequest request) {
        if (!request.isClosed()) {
            if (request.openTime() == null || request.closeTime() == null) {
                throw new ValidationException("Open time and close time are required when site is not closed");
            }
        }
    }

    private OperatingHoursDto toDto(SiteOperatingHours hours) {
        return new OperatingHoursDto(
            hours.getId(),
            hours.getSiteId(),
            hours.getDayOfWeek(),
            hours.isClosed(),
            hours.getOpenTime(),
            hours.getCloseTime()
        );
    }
}
