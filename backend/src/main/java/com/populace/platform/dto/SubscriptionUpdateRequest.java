package com.populace.platform.dto;

import com.populace.domain.enums.SubscriptionStatus;
import com.populace.domain.enums.SubscriptionTier;

public record SubscriptionUpdateRequest(
    SubscriptionTier tier,
    SubscriptionStatus status
) {
}
