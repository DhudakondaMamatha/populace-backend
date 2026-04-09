package com.populace.dashboard.dto;

/**
 * Summary data for the dashboard view.
 */
public record DashboardSummaryDto(
    long totalSites,
    long totalStaff,
    long totalRoles,
    long totalShiftsToday,
    long totalUnallocatedShifts
) {
    public static DashboardSummaryDto empty() {
        return new DashboardSummaryDto(0, 0, 0, 0, 0);
    }
}
