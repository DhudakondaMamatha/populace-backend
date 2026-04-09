package com.populace.platform.dto;

import java.util.Map;

public record PlatformAnalyticsDto(
    long totalBusinesses,
    long activeBusinesses,
    long totalUsers,
    long totalStaff,
    Map<String, Long> businessesByTier,
    Map<String, Long> businessesByStatus
) {
}
