package com.populace.platform.dto;

import com.populace.domain.Business;
import com.populace.domain.enums.SubscriptionStatus;
import com.populace.domain.enums.SubscriptionTier;

import java.time.Instant;

public record BusinessDetailDto(
    Long id,
    String name,
    String email,
    String phone,
    String address,
    String city,
    String state,
    String postalCode,
    String country,
    String industry,
    String contactName,
    String contactEmail,
    String contactPhone,
    SubscriptionTier subscriptionTier,
    SubscriptionStatus subscriptionStatus,
    Instant trialEndsAt,
    String billingEmail,
    String timezone,
    String businessCode,
    boolean active,
    long userCount,
    long staffCount,
    long siteCount,
    Instant createdAt
) {
    public static BusinessDetailDto from(Business business, long userCount, long staffCount, long siteCount) {
        return new BusinessDetailDto(
            business.getId(),
            business.getName(),
            business.getEmail(),
            business.getPhone(),
            business.getAddress(),
            business.getCity(),
            business.getState(),
            business.getPostalCode(),
            business.getCountry(),
            business.getIndustry(),
            business.getContactName(),
            business.getContactEmail(),
            business.getContactPhone(),
            business.getSubscriptionTier(),
            business.getSubscriptionStatus(),
            business.getTrialEndsAt(),
            business.getBillingEmail(),
            business.getTimezone(),
            business.getBusinessCode(),
            !business.isDeleted(),
            userCount,
            staffCount,
            siteCount,
            business.getCreatedAt()
        );
    }
}
