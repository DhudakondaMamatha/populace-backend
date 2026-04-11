package com.populace.schedule.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ShiftAllocationDto(
    Long id,
    Long shiftId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    BigDecimal totalHours,
    Integer breakMinutes,
    List<BreakPeriodDto> breaks,
    Long roleId,
    String roleName,
    Long siteId,
    String siteName
) {}
