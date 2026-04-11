package com.populace.staff.dto;

import com.populace.domain.enums.SkillLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a role assignment with skill level.
 * Used for creating staff with per-role skill level configuration.
 * Skill level is used in allocation ranking (L3 > L2 > L1).
 */
public record RoleAssignment(
    Long roleId,
    String skillLevel,
    String proficiencyLevel,
    Boolean isPrimary,
    Integer minBreakMinutes,
    Integer maxBreakMinutes,
    Integer minWorkMinutesBeforeBreak,
    Integer maxContinuousWorkMinutes
) {
    private static final Logger log = LoggerFactory.getLogger(RoleAssignment.class);

    /**
     * Constructor for backward compatibility (4-arg: no break overrides).
     */
    public RoleAssignment(Long roleId, String skillLevel, String proficiencyLevel, Boolean isPrimary) {
        this(roleId, skillLevel, proficiencyLevel, isPrimary, null, null, null, null);
    }

    /**
     * Constructor for backward compatibility (proficiencyLevel only).
     */
    public RoleAssignment(Long roleId, String proficiencyLevel) {
        this(roleId, mapProficiencyToSkill(proficiencyLevel), proficiencyLevel, null, null, null, null, null);
        if (proficiencyLevel != null) {
            log.warn("Deprecated proficiencyLevel used. Use skillLevel instead.");
        }
    }

    /**
     * Get the resolved skill level (from skillLevel or mapped from proficiencyLevel).
     */
    public SkillLevel getResolvedSkillLevel() {
        if (skillLevel != null) {
            return parseSkillLevel(skillLevel);
        }
        if (proficiencyLevel != null) {
            return SkillLevel.fromProficiencyLevel(proficiencyLevel);
        }
        return SkillLevel.L2;
    }

    private SkillLevel parseSkillLevel(String level) {
        try {
            return SkillLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            return SkillLevel.L2;
        }
    }

    private static String mapProficiencyToSkill(String proficiencyLevel) {
        if (proficiencyLevel == null) return null;
        return switch (proficiencyLevel.toLowerCase()) {
            case "trainee" -> "L1";
            case "competent" -> "L2";
            case "expert" -> "L3";
            default -> null;
        };
    }
}
