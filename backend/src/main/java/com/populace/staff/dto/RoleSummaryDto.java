package com.populace.staff.dto;

/**
 * Summary DTO for role information with ID, name, skill level, and primary flag.
 * Used in StaffDto to enable inline editing with role ID lookup.
 */
public record RoleSummaryDto(
    Long id,
    String name,
    // Skill level for this role assignment: L1, L2, or L3
    String skillLevel,
    // Deprecated: Use skillLevel instead
    String proficiencyLevel,
    // Whether this is a primary role for the staff member
    boolean primary
) {
    /**
     * Constructor for backward compatibility.
     */
    public RoleSummaryDto(Long id, String name) {
        this(id, name, null, null, false);
    }

    /**
     * Constructor with skill level and primary flag.
     */
    public static RoleSummaryDto withSkillLevel(Long id, String name, String skillLevel, boolean primary) {
        String proficiency = mapSkillToProficiency(skillLevel);
        return new RoleSummaryDto(id, name, skillLevel, proficiency, primary);
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
