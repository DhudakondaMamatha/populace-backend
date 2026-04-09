package com.populace.allocation.dto;

public record ReallocateShiftResponse(
    Long shiftId,
    int removedAutoAllocations,
    int manualAllocationsPreserved,
    int newAutoAllocations
) {}
