package com.populace.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Role create/update request - Simplified to metadata only.
 * Constraint fields have been moved to Staff (personal) and Business (system).
 */
public record RoleCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    String code,

    String description,

    @Size(max = 7, message = "Color must be a valid hex code")
    String color,

    Boolean defaultRole
) {}
