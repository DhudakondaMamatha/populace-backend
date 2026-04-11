package com.populace.staff.dto;

/**
 * Summary DTO for site information with both ID and name.
 * Used in StaffDto to enable inline editing with site ID lookup.
 */
public record SiteSummaryDto(
    Long id,
    String name,
    boolean primary
) {}
