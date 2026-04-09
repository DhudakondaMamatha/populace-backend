package com.populace.platform.service;

import com.populace.auth.dto.UserResponse;
import com.populace.auth.jwt.JwtTokenProvider;
import com.populace.common.exception.ValidationException;
import com.populace.domain.Business;
import com.populace.platform.dto.*;
import com.populace.repository.BusinessRepository;
import com.populace.repository.SiteRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformAdminService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminService.class);

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final SiteRepository siteRepository;
    private final JwtTokenProvider tokenProvider;

    public PlatformAdminService(BusinessRepository businessRepository,
                                UserRepository userRepository,
                                StaffMemberRepository staffMemberRepository,
                                SiteRepository siteRepository,
                                JwtTokenProvider tokenProvider) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.siteRepository = siteRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public List<BusinessSummaryDto> listBusinesses() {
        return businessRepository.findAll().stream()
            .map(business -> BusinessSummaryDto.from(
                business,
                countUsers(business.getId()),
                countStaff(business.getId()),
                countSites(business.getId())
            ))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BusinessDetailDto getBusinessDetail(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ValidationException("Business not found"));

        return BusinessDetailDto.from(
            business,
            countUsers(business.getId()),
            countStaff(business.getId()),
            countSites(business.getId())
        );
    }

    @Transactional
    public BusinessDetailDto updateSubscription(Long businessId, SubscriptionUpdateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ValidationException("Business not found"));

        if (request.tier() != null) {
            business.setSubscriptionTier(request.tier());
        }
        if (request.status() != null) {
            business.setSubscriptionStatus(request.status());
        }
        businessRepository.save(business);

        log.info("Subscription updated for businessId={} tier={} status={}",
            businessId, request.tier(), request.status());

        return BusinessDetailDto.from(
            business,
            countUsers(business.getId()),
            countStaff(business.getId()),
            countSites(business.getId())
        );
    }

    @Transactional
    public BusinessDetailDto updateBusinessStatus(Long businessId, BusinessStatusRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ValidationException("Business not found"));

        if (request.active()) {
            business.setDeletedAt(null);
        } else {
            business.setDeletedAt(Instant.now());
        }
        businessRepository.save(business);

        log.info("Business status updated. businessId={} active={}", businessId, request.active());

        return BusinessDetailDto.from(
            business,
            countUsers(business.getId()),
            countStaff(business.getId()),
            countSites(business.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getBusinessUsers(Long businessId) {
        businessRepository.findById(businessId)
            .orElseThrow(() -> new ValidationException("Business not found"));

        return userRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImpersonationResponse impersonate(Long platformAdminId, Long businessId) {
        Business business = businessRepository.findByIdAndDeletedAtIsNull(businessId)
            .orElseThrow(() -> new ValidationException("Business not found or inactive"));

        String token = tokenProvider.generateImpersonationToken(
            platformAdminId, businessId, business.getEmail());

        log.info("Impersonation started. adminId={} businessId={}", platformAdminId, businessId);

        return new ImpersonationResponse(token, business.getId(), business.getName());
    }

    @Transactional(readOnly = true)
    public PlatformAnalyticsDto getAnalytics() {
        List<Business> allBusinesses = businessRepository.findAll();

        long totalBusinesses = allBusinesses.size();
        long activeBusinesses = allBusinesses.stream().filter(b -> !b.isDeleted()).count();
        long totalUsers = userRepository.count();
        long totalStaff = staffMemberRepository.count();

        Map<String, Long> byTier = allBusinesses.stream()
            .filter(b -> !b.isDeleted())
            .collect(Collectors.groupingBy(
                b -> b.getSubscriptionTier().name(),
                Collectors.counting()));

        Map<String, Long> byStatus = allBusinesses.stream()
            .filter(b -> !b.isDeleted())
            .collect(Collectors.groupingBy(
                b -> b.getSubscriptionStatus().name(),
                Collectors.counting()));

        return new PlatformAnalyticsDto(
            totalBusinesses, activeBusinesses, totalUsers, totalStaff, byTier, byStatus);
    }

    private long countUsers(Long businessId) {
        return userRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).size();
    }

    private long countStaff(Long businessId) {
        return staffMemberRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    private long countSites(Long businessId) {
        return siteRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
    }
}
