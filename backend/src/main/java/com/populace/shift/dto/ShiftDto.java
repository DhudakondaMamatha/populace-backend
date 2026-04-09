package com.populace.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftDto(
    Long id,
    Long siteId,
    String siteName,
    Long roleId,
    String roleName,
    LocalDate shiftDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer breakDurationMinutes,
    BigDecimal totalHours,
    Integer staffRequired,
    Integer staffAllocated,
    String status,
    String notes,
    BigDecimal fillRate
) {}
