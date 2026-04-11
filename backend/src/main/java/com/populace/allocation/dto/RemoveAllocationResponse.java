package com.populace.allocation.dto;

public record RemoveAllocationResponse(
    boolean success,
    Long allocationId,
    String message
) {}
