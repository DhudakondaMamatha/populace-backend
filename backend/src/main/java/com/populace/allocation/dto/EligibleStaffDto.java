package com.populace.allocation.dto;

import java.util.List;

public record EligibleStaffDto(
    ShiftDetailsDto shiftDetails,
    List<AllocatedStaffDto> alreadyAllocated,
    List<EligiblePersonDto> eligibleStaff,
    List<IneligiblePersonDto> ineligibleStaff
) {
    public record ShiftDetailsDto(
        String siteName,
        String roleName,
        String shiftDate,
        String startTime,
        String endTime,
        int staffRequired,
        int staffAllocated,
        int remainingSlots
    ) {}

    public record AllocatedStaffDto(
        Long allocationId,
        Long staffId,
        String staffName,
        String allocationType,
        String status
    ) {}

    public record EligiblePersonDto(
        Long staffId,
        String staffName,
        double score,
        boolean fullyEligible,
        List<String> softViolations
    ) {}

    public record IneligiblePersonDto(
        Long staffId,
        String staffName,
        String reason,
        List<String> hardViolations
    ) {}
}
