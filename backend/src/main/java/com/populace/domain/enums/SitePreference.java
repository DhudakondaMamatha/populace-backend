package com.populace.domain.enums;

import java.math.BigDecimal;

/**
 * Site preference levels for staff members.
 * Used in weighted ranking algorithm for allocation scoring.
 *
 * Ranking multipliers per v2.1 specification:
 * - PRIMARY: 1.0 (full weight)
 * - SECONDARY: 0.7 (30% penalty)
 * - AVOIDED: 0.3 (70% penalty)
 */
public enum SitePreference {
    primary(new BigDecimal("1.0")),
    secondary(new BigDecimal("0.7")),
    avoided(new BigDecimal("0.3"));

    private final BigDecimal rankingMultiplier;

    SitePreference(BigDecimal rankingMultiplier) {
        this.rankingMultiplier = rankingMultiplier;
    }

    public BigDecimal getRankingMultiplier() {
        return rankingMultiplier;
    }

    /**
     * Convert legacy boolean is_primary to SitePreference.
     * Used for backward compatibility during migration.
     */
    public static SitePreference fromLegacyPrimary(boolean isPrimary) {
        return isPrimary ? primary : secondary;
    }
}
