package com.populace.signup.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate a cryptographically secure 6-digit OTP.
     */
    public String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    /**
     * Verify OTP matches (constant-time comparison to prevent timing attacks).
     */
    public boolean verifyOtp(String provided, String stored) {
        if (provided == null || stored == null) {
            return false;
        }
        if (provided.length() != stored.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < provided.length(); i++) {
            result |= provided.charAt(i) ^ stored.charAt(i);
        }
        return result == 0;
    }
}
