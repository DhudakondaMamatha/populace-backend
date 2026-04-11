package com.populace.permission;

import com.populace.domain.enums.PermissionLevel;
import org.springframework.stereotype.Component;

/**
 * Checks what actions a permission level allows.
 */
@Component
public class PermissionChecker {

    public boolean canManageUsers(PermissionLevel level) {
        return level == PermissionLevel.ADMIN;
    }

    public boolean canManageBusinessSettings(PermissionLevel level) {
        return level == PermissionLevel.ADMIN;
    }

    public boolean canManageStaff(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER;
    }

    public boolean canManageShifts(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER;
    }

    public boolean canManageSites(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER;
    }

    public boolean canManageLeave(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER;
    }

    public boolean canRunAllocation(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER
            || level == PermissionLevel.EXECUTOR;
    }

    public boolean canViewData(PermissionLevel level) {
        return true; // All levels can view
    }

    public boolean canExportReports(PermissionLevel level) {
        return level == PermissionLevel.ADMIN
            || level == PermissionLevel.MANAGER;
    }
}
