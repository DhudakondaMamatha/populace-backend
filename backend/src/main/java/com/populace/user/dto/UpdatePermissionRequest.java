package com.populace.user.dto;

import com.populace.domain.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;

public record UpdatePermissionRequest(
    @NotNull(message = "Permission level is required")
    PermissionLevel permissionLevel
) {
}
