package com.populace.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleComboCreateRequest(
    @NotBlank(message = "Name is required")
    String name,

    String color,

    @NotNull(message = "At least 2 roles are required")
    @Size(min = 2, message = "At least 2 roles are required")
    List<Long> roleIds
) {}
