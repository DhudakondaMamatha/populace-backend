package com.populace.allocation.dto;

import java.math.BigDecimal;

public record ManualAllocationResponse(
    boolean success,
    Long allocationId,
    Long shiftId,
    Long staffId,
    String staffName,
    BigDecimal hourlyRate,
    BigDecimal estimatedCost,
    boolean overrideApplied
) {}
