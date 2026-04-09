package com.populace.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record SiteCreateRequest(
    @NotBlank(message = "Site name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    String address,

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(max = 100, message = "State must not exceed 100 characters")
    String state,

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    String postalCode,

    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country,

    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    String contactName,

    @Email(message = "Contact email must be valid")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    String contactEmail,

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    String contactPhone,

    Boolean enforceSameRoleBreakExclusivity
) {}
