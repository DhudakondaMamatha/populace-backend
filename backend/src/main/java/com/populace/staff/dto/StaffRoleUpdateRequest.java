package com.populace.staff.dto;

public record StaffRoleUpdateRequest(
    String competenceLevel,
    Integer minBreakMinutes,
    Integer maxBreakMinutes,
    Integer maxBreakDurationMinutes,
    Integer minWorkMinutesBeforeBreak,
    Integer maxContinuousWorkMinutes
) {}
