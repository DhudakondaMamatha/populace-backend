package com.populace.leave.dto;

public record LeaveTypeDto(
    Long id,
    String name,
    String code,
    boolean paid,
    boolean requiresApproval,
    Integer minNoticeDays,
    String color
) {}
