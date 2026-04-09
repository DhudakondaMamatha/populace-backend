package com.populace.schedule.dto;

import java.util.List;

public record StaffScheduleDto(
    Long staffId,
    String staffName,
    List<ScheduleRoleDto> roles,
    List<ShiftAllocationDto> shifts,
    WeeklySummaryDto weeklySummary
) {}
