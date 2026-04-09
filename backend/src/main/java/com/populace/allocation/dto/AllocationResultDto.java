package com.populace.allocation.dto;

public record AllocationResultDto(
    String runId,
    int totalShifts,
    int shiftsFilled,
    int shiftsPartial,
    int shiftsUnfilled,
    int blocksCreated,
    int totalAllocations,
    String status,
    String dateRangeStart,
    String dateRangeEnd
) {}
