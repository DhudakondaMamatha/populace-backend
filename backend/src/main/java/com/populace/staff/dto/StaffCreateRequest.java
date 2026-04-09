package com.populace.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record StaffCreateRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    String phone,
    String employeeCode,
    String employmentType,
    List<Long> roleIds,
    List<Long> siteIds
) {}
