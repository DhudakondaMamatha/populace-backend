package com.populace.auth.util;

import com.populace.auth.service.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for accessing current authenticated user information.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static Long getCurrentUserId() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null ? principal.getId() : null;
    }

    public static Long getCurrentBusinessId() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null ? principal.getBusinessId() : null;
    }

    public static String getCurrentUserEmail() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null ? principal.getUsername() : null;
    }

    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }

        return null;
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
               && authentication.getPrincipal() instanceof UserPrincipal;
    }

    public static boolean isPlatformAdmin() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null && principal.isPlatformAdmin();
    }

    public static boolean isImpersonating() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null && principal.isImpersonating();
    }
}
