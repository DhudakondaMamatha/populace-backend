package com.populace.allocation.dto;

import java.util.List;

public record ShiftAllocationsDto(
    Long shiftId,
    String siteName,
    String roleName,
    int staffRequired,
    int staffAllocated,
    List<AllocationItemDto> allocations
) {
    public record AllocationItemDto(
        Long allocationId,
        Long staffId,
        String staffName,
        String allocationType,
        String status
    ) {}
}
