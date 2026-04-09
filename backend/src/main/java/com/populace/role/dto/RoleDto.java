package com.populace.role.dto;

/**
 * Role DTO - Simplified to metadata only.
 * Constraint fields have been moved to Staff (personal) and Business (system).
 */
public record RoleDto(
    Long id,
    String name,
    String code,
    String description,
    String color,
    boolean defaultRole,
    boolean active
) {}
