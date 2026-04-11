package com.populace.shift.dto;

import java.time.LocalTime;

public record ScheduleTemplateEntryDto(
    Long roleId,
    String roleName,
    String roleColor,
    Long roleComboId,
    String roleComboName,
    String roleComboColor,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int breakDurationMinutes,
    int staffRequired
) {}
