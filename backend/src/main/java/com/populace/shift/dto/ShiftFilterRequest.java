package com.populace.shift.dto;

import java.time.LocalDate;

public record ShiftFilterRequest(
    LocalDate startDate,
    LocalDate endDate,
    Long siteId,
    Long roleId,
    String status
) {}
