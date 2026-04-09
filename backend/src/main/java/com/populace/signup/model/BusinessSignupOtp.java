package com.populace.signup.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_signup_otps")
public class BusinessSignupOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "verification_token", nullable = false, unique = true)
    private String verificationToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    public BusinessSignupOtp() {
    }

    public static BusinessSignupOtp create(String email, String otpCode, int expirationMinutes) {
        BusinessSignupOtp otp = new BusinessSignupOtp();
        otp.email = email.toLowerCase().trim();
        otp.otpCode = otpCode;
        otp.verificationToken = UUID.randomUUID().toString();
        otp.expiresAt = Instant.now().plusSeconds(expirationMinutes * 60L);
        otp.attempts = 0;
        return otp;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean hasExceededAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markVerified() {
        this.verifiedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
