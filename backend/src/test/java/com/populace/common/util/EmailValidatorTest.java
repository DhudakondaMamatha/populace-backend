package com.populace.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BUG #8: Email Validation Consistency
 * Verifies unified email validation behavior.
 */
@DisplayName("Email Validator Tests")
class EmailValidatorTest {

    @ParameterizedTest
    @DisplayName("Valid emails should pass validation")
    @ValueSource(strings = {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.com",
        "user_name@example.co.uk",
        "user-name@sub.domain.com",
        "USER@EXAMPLE.COM"
    })
    void validEmailsShouldPass(String email) {
        assertTrue(EmailValidator.isValid(email), "Email should be valid: " + email);
    }

    @ParameterizedTest
    @DisplayName("Invalid emails should fail validation")
    @ValueSource(strings = {
        "user@domain",           // No TLD
        "user",                  // No @ symbol
        "@example.com",          // No local part
        "user@",                 // No domain
        "user@.com",             // Invalid domain
        "user space@example.com" // Space in email
    })
    void invalidEmailsShouldFail(String email) {
        assertFalse(EmailValidator.isValid(email), "Email should be invalid: " + email);
    }

    @Test
    @DisplayName("Null email should fail validation")
    void nullEmailShouldFail() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    @DisplayName("Empty email should fail validation")
    void emptyEmailShouldFail() {
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    @DisplayName("Blank email should fail validation")
    void blankEmailShouldFail() {
        assertFalse(EmailValidator.isValid("   "));
    }

    @Test
    @DisplayName("isValidOrEmpty should accept null")
    void isValidOrEmptyShouldAcceptNull() {
        assertTrue(EmailValidator.isValidOrEmpty(null));
    }

    @Test
    @DisplayName("isValidOrEmpty should accept empty string")
    void isValidOrEmptyShouldAcceptEmptyString() {
        assertTrue(EmailValidator.isValidOrEmpty(""));
    }

    @Test
    @DisplayName("isValidOrEmpty should accept blank string")
    void isValidOrEmptyShouldAcceptBlankString() {
        assertTrue(EmailValidator.isValidOrEmpty("   "));
    }

    @Test
    @DisplayName("isValidOrEmpty should reject invalid email")
    void isValidOrEmptyShouldRejectInvalidEmail() {
        assertFalse(EmailValidator.isValidOrEmpty("invalid"));
    }

    @Test
    @DisplayName("isValidOrEmpty should accept valid email")
    void isValidOrEmptyShouldAcceptValidEmail() {
        assertTrue(EmailValidator.isValidOrEmpty("user@example.com"));
    }

    @Test
    @DisplayName("TLD must be at least 2 characters")
    void tldMustBeAtLeast2Characters() {
        assertFalse(EmailValidator.isValid("user@domain.a"));
        assertTrue(EmailValidator.isValid("user@domain.ab"));
    }
}
