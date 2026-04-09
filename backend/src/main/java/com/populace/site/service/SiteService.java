package com.populace.site.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.Business;
import com.populace.domain.Site;
import com.populace.repository.BusinessRepository;
import com.populace.repository.SiteRepository;
import com.populace.site.dto.SiteCreateRequest;
import com.populace.site.dto.SiteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final BusinessRepository businessRepository;

    public SiteService(SiteRepository siteRepository,
                       BusinessRepository businessRepository) {
        this.siteRepository = siteRepository;
        this.businessRepository = businessRepository;
    }

    public List<SiteDto> listSites(Long businessId, String status, String search) {
        return siteRepository.findByBusiness_IdAndDeletedAtIsNull(businessId)
            .stream()
            .filter(site -> matchesFilters(site, status, search))
            .map(this::toDto)
            .toList();
    }

    public SiteDto getSiteById(Long businessId, Long siteId) {
        Site site = getSiteOrThrow(businessId, siteId);
        return toDto(site);
    }

    @Transactional
    public SiteDto createSite(Long businessId, SiteCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        Site site = new Site();
        site.setBusiness(business);
        site.setActive(true);
        applyRequestToSite(site, request);

        site = siteRepository.save(site);
        return toDto(site);
    }

    @Transactional
    public SiteDto updateSite(Long businessId, Long siteId, SiteCreateRequest request) {
        Site site = getSiteOrThrow(businessId, siteId);
        applyRequestToSite(site, request);

        site = siteRepository.save(site);
        return toDto(site);
    }

    @Transactional
    public void deactivateSite(Long businessId, Long siteId) {
        Site site = getSiteOrThrow(businessId, siteId);
        site.setActive(false);
        siteRepository.save(site);
    }

    @Transactional
    public void activateSite(Long businessId, Long siteId) {
        Site site = getSiteOrThrow(businessId, siteId);
        site.setActive(true);
        siteRepository.save(site);
    }

    private Site getSiteOrThrow(Long businessId, Long siteId) {
        return siteRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(siteId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Site", siteId));
    }

    private boolean matchesFilters(Site site, String status, String search) {
        if (status != null && !status.isEmpty()) {
            boolean isActive = "active".equalsIgnoreCase(status);
            if (site.isActive() != isActive) {
                return false;
            }
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            boolean nameMatches = site.getName() != null &&
                site.getName().toLowerCase().contains(searchLower);
            boolean addressMatches = site.getAddress() != null &&
                site.getAddress().toLowerCase().contains(searchLower);
            return nameMatches || addressMatches;
        }

        return true;
    }

    private void applyRequestToSite(Site site, SiteCreateRequest request) {
        site.setName(request.name());
        site.setCode(request.code());
        site.setAddress(request.address());
        site.setCity(request.city());
        site.setState(request.state());
        site.setPostalCode(request.postalCode());
        site.setCountry(request.country());
        site.setContactName(request.contactName());
        site.setContactEmail(request.contactEmail());
        site.setContactPhone(request.contactPhone());
        site.setEnforceSameRoleBreakExclusivity(
            request.enforceSameRoleBreakExclusivity() != null
                ? request.enforceSameRoleBreakExclusivity()
                : true
        );
    }

    private SiteDto toDto(Site site) {
        return new SiteDto(
            site.getId(),
            site.getName(),
            site.getCode(),
            site.getAddress(),
            site.getCity(),
            site.getState(),
            site.getPostalCode(),
            site.getCountry(),
            site.getContactName(),
            site.getContactEmail(),
            site.getContactPhone(),
            site.isActive(),
            site.isEnforceSameRoleBreakExclusivity()
        );
    }
}
