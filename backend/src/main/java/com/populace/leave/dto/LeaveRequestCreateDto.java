package com.populace.leave.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestCreateDto(
    @NotNull Long staffId,
    @NotNull Long leaveTypeId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    BigDecimal totalDays,
    String reason
) {}
