package com.populace.signup.dto;

public record SignupCompleteResponse(
    Long businessId,
    String businessCode,
    String businessName,
    String email,
    String message
) {}
