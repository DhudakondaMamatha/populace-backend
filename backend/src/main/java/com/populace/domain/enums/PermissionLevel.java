package com.populace.domain.enums;

/**
 * Permission levels for user access control.
 * Ordered from highest to lowest access.
 */
public enum PermissionLevel {
    ADMIN,      // Full access to all features
    MANAGER,    // Can manage staff, shifts, allocations
    EXECUTOR,   // Can run allocations, view data
    VIEWER      // Read-only access
}
