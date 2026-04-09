package com.populace.platform.dto;

public record ImpersonationResponse(
    String token,
    Long businessId,
    String businessName
) {
}
