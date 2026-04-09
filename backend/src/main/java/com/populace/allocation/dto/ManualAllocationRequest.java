package com.populace.allocation.dto;

public record ManualAllocationRequest(
    Long staffId,
    boolean overrideViolations,
    String overrideReason
) {}
