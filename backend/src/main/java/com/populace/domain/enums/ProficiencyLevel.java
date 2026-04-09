package com.populace.domain.enums;

/**
 * Proficiency levels for staff competencies.
 * Ordered from lowest (trainee=1) to highest (expert=3).
 *
 * Used in eligibility checks: staff level must meet or exceed role requirement.
 */
public enum ProficiencyLevel {
    trainee(1),
    competent(2),
    expert(3);

    private final int level;

    ProficiencyLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this level meets or exceeds the required level.
     * Per v2.1 spec: Hard Constraint #5 - staff.competencyLevel >= role.requiredLevel
     *
     * @param required the minimum required level
     * @return true if this level is sufficient
     */
    public boolean meetsOrExceeds(ProficiencyLevel required) {
        if (required == null) {
            return true; // No requirement means any level is acceptable
        }
        return this.level >= required.level;
    }

    /**
     * Calculate how many levels above the required level.
     * Used for ranking bonus calculations.
     *
     * @param required the baseline level
     * @return number of levels above (0 if equal or below)
     */
    public int levelsAbove(ProficiencyLevel required) {
        if (required == null) {
            return this.level;
        }
        return Math.max(0, this.level - required.level);
    }
}
