package com.populace.auth.dto;

import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;
import com.populace.platform.domain.PlatformAdmin;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String userType,
    PermissionLevel permissionLevel,
    Long businessId,
    String businessName,
    boolean platformAdmin
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getUserType().name(),
            user.getPermissionLevel(),
            user.getBusinessId(),
            user.getBusiness() != null ? user.getBusiness().getName() : null,
            false
        );
    }

    public static UserResponse fromPlatformAdmin(PlatformAdmin admin) {
        return new UserResponse(
            admin.getId(),
            admin.getEmail(),
            admin.getFirstName(),
            admin.getLastName(),
            "platform_admin",
            null,
            null,
            null,
            true
        );
    }
}
