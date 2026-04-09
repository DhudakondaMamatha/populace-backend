package com.populace.signup.service;

import com.populace.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String OTP_SUBJECT = "Your Populace Verification Code";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send OTP verification email for signup.
     *
     * @param toEmail recipient email address
     * @param otpCode 6-digit OTP code
     * @throws ValidationException if email sending fails
     */
    public void sendSignupOtp(String toEmail, String otpCode) {
        String body = String.format(
            "Your verification code is: %s\n\nThis code expires in 10 minutes.",
            otpCode
        );

        sendEmail(toEmail, OTP_SUBJECT, body);
    }

    /**
     * Send a plain text email.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     * @throws ValidationException if email sending fails
     */
    private void sendEmail(String to, String subject, String body) {
        // Dev mode: log email instead of sending when not configured
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("===========================================");
            log.warn("DEV MODE - Email not configured");
            log.warn("To: {}", to);
            log.warn("Subject: {}", subject);
            log.warn("Body: {}", body);
            log.warn("===========================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new ValidationException("Failed to send verification email. Please try again.");
        }
    }
}
