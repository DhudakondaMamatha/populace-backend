package com.populace.platform.dto;

import com.populace.domain.Business;
import com.populace.domain.enums.SubscriptionStatus;
import com.populace.domain.enums.SubscriptionTier;

import java.time.Instant;

public record BusinessSummaryDto(
    Long id,
    String name,
    String email,
    SubscriptionTier subscriptionTier,
    SubscriptionStatus subscriptionStatus,
    long userCount,
    long staffCount,
    long siteCount,
    boolean active,
    Instant createdAt
) {
    public static BusinessSummaryDto from(Business business, long userCount, long staffCount, long siteCount) {
        return new BusinessSummaryDto(
            business.getId(),
            business.getName(),
            business.getEmail(),
            business.getSubscriptionTier(),
            business.getSubscriptionStatus(),
            userCount,
            staffCount,
            siteCount,
            !business.isDeleted(),
            business.getCreatedAt()
        );
    }
}
