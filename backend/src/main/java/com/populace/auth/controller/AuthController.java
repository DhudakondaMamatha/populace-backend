package com.populace.auth.controller;

import com.populace.auth.dto.LoginRequest;
import com.populace.auth.dto.LoginResponse;
import com.populace.auth.dto.RegisterRequest;
import com.populace.auth.dto.UserResponse;
import com.populace.auth.service.AuthService;
import com.populace.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.isPlatformAdmin() && !principal.isImpersonating()) {
            UserResponse user = authService.getPlatformAdminById(principal.getId());
            return ResponseEntity.ok(user);
        }
        UserResponse user = authService.getUserById(principal.getId());
        return ResponseEntity.ok(user);
    }
}
