package com.populace.common.util;

import java.util.regex.Pattern;

/**
 * Unified email validation utility.
 * Provides consistent email validation across manual and bulk staff creation.
 *
 * Pattern requirements:
 * - Local part: alphanumeric, plus (+), underscore (_), dot (.), hyphen (-)
 * - Domain: alphanumeric, dot (.), hyphen (-)
 * - TLD: minimum 2 characters
 */
public final class EmailValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private EmailValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates an email address format.
     *
     * @param email The email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates an email address format, allowing null/blank as valid (optional field).
     *
     * @param email The email address to validate
     * @return true if the email is null, blank, or valid format
     */
    public static boolean isValidOrEmpty(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
