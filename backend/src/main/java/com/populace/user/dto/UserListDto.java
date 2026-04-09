package com.populace.user.dto;

import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;

import java.time.Instant;

public record UserListDto(
    Long id,
    String email,
    String firstName,
    String lastName,
    PermissionLevel permissionLevel,
    boolean active,
    Instant lastLoginAt
) {
    public static UserListDto from(User user) {
        return new UserListDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPermissionLevel(),
            user.isActive(),
            user.getLastLoginAt()
        );
    }
}
