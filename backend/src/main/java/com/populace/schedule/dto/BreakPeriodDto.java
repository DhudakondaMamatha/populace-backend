package com.populace.schedule.dto;

import java.time.LocalTime;

public record BreakPeriodDto(
    LocalTime startTime,
    LocalTime endTime
) {}
