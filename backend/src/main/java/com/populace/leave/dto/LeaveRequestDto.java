package com.populace.leave.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestDto(
    Long id,
    Long staffId,
    String staffName,
    Long leaveTypeId,
    String leaveTypeName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal totalDays,
    String reason,
    String status,
    String reviewedByName,
    Instant reviewedAt,
    String reviewNotes,
    Instant createdAt
) {}
