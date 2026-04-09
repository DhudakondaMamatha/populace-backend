package com.populace.staff.dto;

/**
 * DTO for staff role assignment details.
 * Includes skill level for allocation ranking.
 */
public record StaffRoleDto(
    Long id,
    Long roleId,
    String roleName,
    // Skill level: L1, L2, or L3
    String skillLevel,
    // Deprecated: Use skillLevel instead
    String proficiencyLevel,
    boolean primary,
    // Deprecated: Competence levels are now per-role via skillLevel
    String competenceLevel,
    Integer minBreakMinutes,
    Integer maxBreakMinutes,
    Integer maxBreakDurationMinutes,
    Integer effectiveMinBreakMinutes,
    Integer effectiveMaxBreakMinutes,
    Integer effectiveMaxBreakDurationMinutes,
    Integer minWorkMinutesBeforeBreak,
    Integer maxContinuousWorkMinutes,
    Integer effectiveMinWorkMinutesBeforeBreak,
    Integer effectiveMaxContinuousWorkMinutes,
    boolean hasBreakOverride,
    boolean active
) {
    /**
     * Create DTO with skill level (preferred).
     */
    public static StaffRoleDto withSkillLevel(
            Long id,
            Long roleId,
            String roleName,
            String skillLevel,
            boolean primary,
            Integer minBreakMinutes,
            Integer maxBreakMinutes,
            Integer maxBreakDurationMinutes,
            Integer effectiveMinBreakMinutes,
            Integer effectiveMaxBreakMinutes,
            Integer effectiveMaxBreakDurationMinutes,
            Integer minWorkMinutesBeforeBreak,
            Integer maxContinuousWorkMinutes,
            Integer effectiveMinWorkMinutesBeforeBreak,
            Integer effectiveMaxContinuousWorkMinutes,
            boolean hasBreakOverride,
            boolean active) {

        String proficiency = mapSkillToProficiency(skillLevel);

        return new StaffRoleDto(
            id, roleId, roleName, skillLevel, proficiency, primary, null,
            minBreakMinutes, maxBreakMinutes, maxBreakDurationMinutes,
            effectiveMinBreakMinutes, effectiveMaxBreakMinutes,
            effectiveMaxBreakDurationMinutes,
            minWorkMinutesBeforeBreak, maxContinuousWorkMinutes,
            effectiveMinWorkMinutesBeforeBreak, effectiveMaxContinuousWorkMinutes,
            hasBreakOverride, active
        );
    }

    private static String mapSkillToProficiency(String skillLevel) {
        if (skillLevel == null) return null;
        return switch (skillLevel) {
            case "L1" -> "trainee";
            case "L2" -> "competent";
            case "L3" -> "expert";
            default -> null;
        };
    }
}
