package com.populace.permission;

import com.populace.auth.service.UserPrincipal;
import com.populace.domain.enums.PermissionLevel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Guards endpoints by checking user permission levels.
 * Throws AccessDeniedException when access is denied.
 */
@Component
public class PermissionGuard {

    private final PermissionChecker checker;

    public PermissionGuard(PermissionChecker checker) {
        this.checker = checker;
    }

    public void requireAdmin(UserPrincipal user) {
        ensureAuthenticated(user);
        if (user.getPermissionLevel() != PermissionLevel.ADMIN) {
            throw new AccessDeniedException("Administrator access required");
        }
    }

    public void requireManagerOrAbove(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canManageStaff(user.getPermissionLevel())) {
            throw new AccessDeniedException("Manager access required");
        }
    }

    public void requireExecutorOrAbove(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canRunAllocation(user.getPermissionLevel())) {
            throw new AccessDeniedException("Executor access required");
        }
    }

    public void requireStaffManagement(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canManageStaff(user.getPermissionLevel())) {
            throw new AccessDeniedException("Staff management access required");
        }
    }

    public void requireShiftManagement(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canManageShifts(user.getPermissionLevel())) {
            throw new AccessDeniedException("Shift management access required");
        }
    }

    public void requireAllocationAccess(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canRunAllocation(user.getPermissionLevel())) {
            throw new AccessDeniedException("Allocation access required");
        }
    }

    public void requireLeaveManagement(UserPrincipal user) {
        ensureAuthenticated(user);
        if (!checker.canManageLeave(user.getPermissionLevel())) {
            throw new AccessDeniedException("Leave management access required");
        }
    }

    private void ensureAuthenticated(UserPrincipal user) {
        if (user == null) {
            throw new AccessDeniedException("User not authenticated");
        }
    }
}
