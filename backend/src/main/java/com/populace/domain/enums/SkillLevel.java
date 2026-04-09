package com.populace.domain.enums;

/**
 * Skill levels for staff role assignments.
 * Used in allocation ranking (L3 highest priority) and eligibility checks.
 *
 * L1 = Entry level - Basic competence, supervised work
 * L2 = Intermediate - Full competence, independent work
 * L3 = Advanced - Expert level, can train others
 */
public enum SkillLevel {
    L1(1),
    L2(2),
    L3(3);

    private final int numericLevel;

    SkillLevel(int numericLevel) {
        this.numericLevel = numericLevel;
    }

    public int getNumericLevel() {
        return numericLevel;
    }

    /**
     * Check if this level meets or exceeds the required level.
     */
    public boolean meetsOrExceeds(SkillLevel required) {
        if (required == null) {
            return true;
        }
        return this.numericLevel >= required.numericLevel;
    }

    /**
     * Calculate how many levels above the required level.
     */
    public int levelsAbove(SkillLevel required) {
        if (required == null) {
            return this.numericLevel;
        }
        return Math.max(0, this.numericLevel - required.numericLevel);
    }

    /**
     * Convert from deprecated ProficiencyLevel string value.
     */
    public static SkillLevel fromProficiencyLevel(String proficiencyLevel) {
        if (proficiencyLevel == null) {
            return null;
        }
        return switch (proficiencyLevel.toLowerCase()) {
            case "trainee" -> L1;
            case "competent" -> L2;
            case "expert" -> L3;
            default -> null;
        };
    }

    /**
     * Convert to deprecated ProficiencyLevel string value.
     */
    public String toProficiencyLevel() {
        return switch (this) {
            case L1 -> "trainee";
            case L2 -> "competent";
            case L3 -> "expert";
        };
    }
}
