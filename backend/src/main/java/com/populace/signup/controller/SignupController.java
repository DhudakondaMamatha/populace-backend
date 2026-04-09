package com.populace.signup.controller;

import com.populace.domain.Business;
import com.populace.signup.dto.*;
import com.populace.signup.service.BusinessSignupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final BusinessSignupService signupService;

    public SignupController(BusinessSignupService signupService) {
        this.signupService = signupService;
    }

    /**
     * Step 1: Initiate signup - send OTP to email
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiateSignup(
            @Valid @RequestBody SignupInitiateRequest request) {
        signupService.initiateSignup(request);
        return ResponseEntity.ok(Map.of(
            "message", "Verification code sent to your email"
        ));
    }

    /**
     * Step 2: Verify OTP - returns verification token
     */
    @PostMapping("/verify")
    public ResponseEntity<SignupVerifyResponse> verifyOtp(
            @Valid @RequestBody SignupVerifyRequest request) {
        SignupVerifyResponse response = signupService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: Complete signup - create business and admin user
     */
    @PostMapping("/complete")
    public ResponseEntity<SignupCompleteResponse> completeSignup(
            @Valid @RequestBody SignupCompleteRequest request) {
        Business business = signupService.completeSignup(request);

        SignupCompleteResponse response = new SignupCompleteResponse(
            business.getId(),
            business.getBusinessCode(),
            business.getName(),
            business.getEmail(),
            "Account created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Resend OTP (same as initiate)
     */
    @PostMapping("/resend")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody SignupInitiateRequest request) {
        signupService.initiateSignup(request);
        return ResponseEntity.ok(Map.of(
            "message", "New verification code sent to your email"
        ));
    }
}
