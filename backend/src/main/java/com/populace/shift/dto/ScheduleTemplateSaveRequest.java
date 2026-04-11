package com.populace.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record ScheduleTemplateSaveRequest(
    @NotNull Long siteId,
    String name,
    @Valid List<Entry> entries
) {
    public record Entry(
        Long roleId,
        Long roleComboId,
        @NotNull Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer breakDurationMinutes,
        Integer staffRequired
    ) {}
}
