package com.populace.signup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupCompleteRequest(
    @NotBlank(message = "Verification token is required")
    String verificationToken,

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be 2-255 characters")
    String businessName,

    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    String phone,
    String timezone
) {}
