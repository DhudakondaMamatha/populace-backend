package com.populace.auth.dto;

import com.populace.domain.User;
import com.populace.platform.domain.PlatformAdmin;

public record LoginResponse(
    String token,
    UserResponse user
) {
    public static LoginResponse of(String token, User user) {
        return new LoginResponse(token, UserResponse.from(user));
    }

    public static LoginResponse ofPlatformAdmin(String token, PlatformAdmin admin) {
        return new LoginResponse(token, UserResponse.fromPlatformAdmin(admin));
    }
}
