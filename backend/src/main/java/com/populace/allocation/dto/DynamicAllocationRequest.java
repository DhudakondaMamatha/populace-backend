package com.populace.allocation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for the dynamic shift allocation algorithm.
 *
 * siteIds  — optional list of site IDs to restrict allocation to; null or empty means all active sites.
 * roleIds  — optional list of role IDs to restrict allocation to; null or empty means all active roles.
 * clearExistingAutoAllocations — when true, existing AUTO time blocks in the range are removed before re-running.
 */
public record DynamicAllocationRequest(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        boolean clearExistingAutoAllocations,
        List<Long> siteIds,
        List<Long> roleIds
) {}
