package com.populace.allocation.dto;

public record PreCheckDto(
    int totalShifts,
    int openShifts,
    int partialShifts,
    int filledShifts,
    long autoAllocationsToRemove,
    long manualAllocationsPreserved,
    boolean hasFilledShifts,
    String warningMessage
) {}
