package com.populace.auth.service;

import com.populace.auth.dto.LoginRequest;
import com.populace.auth.dto.LoginResponse;
import com.populace.auth.dto.RegisterRequest;
import com.populace.auth.dto.UserResponse;
import com.populace.auth.jwt.JwtTokenProvider;
import com.populace.common.exception.ValidationException;
import com.populace.domain.Business;
import com.populace.domain.User;
import com.populace.domain.enums.UserType;
import com.populace.platform.domain.PlatformAdmin;
import com.populace.platform.repository.PlatformAdminRepository;
import com.populace.repository.BusinessRepository;
import com.populace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       BusinessRepository businessRepository,
                       PlatformAdminRepository platformAdminRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getPlatformAdminById(Long id) {
        PlatformAdmin admin = platformAdminRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Platform admin not found"));
        return UserResponse.fromPlatformAdmin(admin);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Check platform_admins table first
        Optional<PlatformAdmin> platformAdmin = platformAdminRepository.findByEmailAndActiveTrue(request.email());
        if (platformAdmin.isPresent()) {
            PlatformAdmin admin = platformAdmin.get();
            if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                throw new ValidationException("Invalid email or password");
            }
            admin.setLastLoginAt(Instant.now());
            platformAdminRepository.save(admin);

            String token = tokenProvider.generatePlatformAdminToken(admin.getId(), admin.getEmail());
            log.info("Platform admin logged in. adminId={}", admin.getId());
            return LoginResponse.ofPlatformAdmin(token, admin);
        }

        // Fall through to regular users
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
            .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!user.isActive()) {
            throw new ValidationException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(
            user.getId(), user.getBusinessId(), user.getEmail());

        log.info("User logged in. userId={} businessId={}",
            user.getId(), user.getBusinessId());

        return LoginResponse.of(token, user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmailAndDeletedAtIsNull(request.email()).isPresent()) {
            throw new ValidationException("email", "Email already registered");
        }

        Business business = createBusiness(request);
        User user = createAdminUser(request, business);

        String token = tokenProvider.generateToken(
            user.getId(), business.getId(), user.getEmail());

        log.info("New business registered. businessId={} userId={}",
            business.getId(), user.getId());

        return LoginResponse.of(token, user);
    }

    private Business createBusiness(RegisterRequest request) {
        Business business = new Business();
        business.setName(request.businessName());
        business.setEmail(request.email());
        return businessRepository.save(business);
    }

    private User createAdminUser(RegisterRequest request, Business business) {
        User user = new User();
        user.setBusiness(business);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setUserType(UserType.admin);
        user.setActive(true);
        return userRepository.save(user);
    }
}
