package com.populace.schedule.dto;

/**
 * Filter parameters for schedule queries.
 * All parameters are optional - null means "no filter".
 * NO DATABASE CHANGES REQUIRED.
 */
public record ScheduleFilterParams(
    Long siteId,
    Long roleId,
    String resourceName
) {
    public boolean hasSiteFilter() {
        return siteId != null;
    }

    public boolean hasRoleFilter() {
        return roleId != null;
    }

    public boolean hasResourceNameFilter() {
        return resourceName != null && !resourceName.isBlank();
    }

    public boolean hasAnyFilter() {
        return hasSiteFilter() || hasRoleFilter() || hasResourceNameFilter();
    }

    public String getResourceNameLower() {
        return resourceName != null ? resourceName.toLowerCase().trim() : "";
    }
}
