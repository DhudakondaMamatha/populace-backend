package com.populace.allocation.dto;

import java.util.List;

/**
 * Result returned by the dynamic shift allocation algorithm.
 */
public record DynamicAllocationResultDto(
        String runId,
        String startDate,
        String endDate,

        /** Total shifts considered (generated + pre-existing). */
        int totalShifts,

        /** Shifts created fresh by this run. */
        int shiftsGenerated,

        /** Shifts that already existed and were reused. */
        int shiftsAlreadyExisted,

        /** Individual staff-to-shift assignments made this run. */
        int staffAssigned,

        /** Shifts that reached full staffRequired coverage after this run. */
        int shiftsFullyCovered,

        /** Shifts that still have open slots after this run. */
        int shiftsUncovered,

        String status,
        List<String> warnings
) {}
