package com.populace.role.dto;

import java.util.List;

public record RoleComboDto(
    Long id,
    String name,
    String color,
    boolean active,
    List<RoleComboRoleDto> roles
) {}
