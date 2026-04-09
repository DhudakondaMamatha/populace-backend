package com.populace.signup.service;

import com.populace.common.exception.ValidationException;
import com.populace.domain.Business;
import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;
import com.populace.domain.enums.UserType;
import com.populace.leave.service.LeaveTypeInitializer;
import com.populace.repository.BusinessRepository;
import com.populace.repository.UserRepository;
import com.populace.signup.dto.SignupCompleteRequest;
import com.populace.signup.dto.SignupInitiateRequest;
import com.populace.signup.dto.SignupVerifyRequest;
import com.populace.signup.dto.SignupVerifyResponse;
import com.populace.signup.model.BusinessSignupOtp;
import com.populace.signup.repository.BusinessSignupOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class BusinessSignupService {

    private static final Logger log = LoggerFactory.getLogger(BusinessSignupService.class);

    private static final int OTP_EXPIRATION_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final BusinessSignupOtpRepository otpRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LeaveTypeInitializer leaveTypeInitializer;

    public BusinessSignupService(
            BusinessSignupOtpRepository otpRepository,
            BusinessRepository businessRepository,
            UserRepository userRepository,
            OtpService otpService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            LeaveTypeInitializer leaveTypeInitializer) {
        this.otpRepository = otpRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.leaveTypeInitializer = leaveTypeInitializer;
    }

    @Transactional
    public void initiateSignup(SignupInitiateRequest request) {
        String email = request.email().toLowerCase().trim();

        if (businessRepository.existsByEmail(email)) {
            throw new ValidationException("An account with this email already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("An account with this email already exists");
        }

        otpRepository.deleteByEmail(email);

        String otpCode = otpService.generateOtp();
        BusinessSignupOtp otp = BusinessSignupOtp.create(email, otpCode, OTP_EXPIRATION_MINUTES);
        otpRepository.save(otp);

        emailService.sendSignupOtp(email, otpCode);

        log.info("Signup initiated for email: {}", email);
    }

    @Transactional
    public SignupVerifyResponse verifyOtp(SignupVerifyRequest request) {
        String email = request.email().toLowerCase().trim();

        BusinessSignupOtp otp = otpRepository.findByEmailAndVerifiedAtIsNull(email)
                .orElseThrow(() -> new ValidationException("No pending verification found for this email"));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new ValidationException("OTP has expired. Please request a new one");
        }

        if (otp.hasExceededAttempts(MAX_OTP_ATTEMPTS)) {
            otpRepository.delete(otp);
            throw new ValidationException("Too many failed attempts. Please request a new OTP");
        }

        if (!otpService.verifyOtp(request.otpCode(), otp.getOtpCode())) {
            otp.incrementAttempts();
            otpRepository.save(otp);
            int remaining = MAX_OTP_ATTEMPTS - otp.getAttempts();
            throw new ValidationException("Invalid OTP. " + remaining + " attempts remaining");
        }

        otp.markVerified();
        otpRepository.save(otp);

        log.info("OTP verified for email: {}", email);

        return new SignupVerifyResponse(otp.getVerificationToken());
    }

    @Transactional
    public Business completeSignup(SignupCompleteRequest request) {
        BusinessSignupOtp otp = otpRepository.findByVerificationTokenAndVerifiedAtIsNotNull(
                        request.verificationToken())
                .orElseThrow(() -> new ValidationException("Invalid or expired verification token"));

        if (otp.getVerifiedAt().plus(30, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            otpRepository.delete(otp);
            throw new ValidationException("Verification has expired. Please start over");
        }

        String email = otp.getEmail();

        if (businessRepository.existsByEmail(email)) {
            otpRepository.delete(otp);
            throw new ValidationException("An account with this email already exists");
        }

        Business business = createBusiness(request, email);
        User adminUser = createAdminUser(business, request, email);

        // Initialize default leave types for the new business
        leaveTypeInitializer.initializeDefaultLeaveTypes(business);

        otpRepository.delete(otp);

        log.info("Business signup completed: {} ({})", business.getName(), business.getBusinessCode());

        return business;
    }

    private Business createBusiness(SignupCompleteRequest request, String email) {
        Business business = new Business();
        business.setName(request.businessName());
        business.setEmail(email);
        business.setPhone(request.phone());
        business.setTimezone(request.timezone() != null ? request.timezone() : "UTC");
        business.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        business = businessRepository.save(business);

        String businessCode = generateBusinessCode(business.getId());
        business.setBusinessCode(businessCode);
        return businessRepository.save(business);
    }

    private User createAdminUser(Business business, SignupCompleteRequest request, String email) {
        User admin = new User();
        admin.setBusiness(business);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setFirstName(request.firstName());
        admin.setLastName(request.lastName());
        admin.setUserType(UserType.admin);
        admin.setPermissionLevel(PermissionLevel.ADMIN);
        admin.setActive(true);
        admin.setEmailVerifiedAt(Instant.now());
        return userRepository.save(admin);
    }

    public String generateBusinessCode(Long businessId) {
        return String.format("BIZ-%06d", businessId);
    }

    @Transactional
    public int cleanupExpiredOtps() {
        return otpRepository.deleteExpiredUnverified(Instant.now());
    }
}
