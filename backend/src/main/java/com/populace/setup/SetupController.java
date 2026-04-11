package com.populace.setup;

import com.populace.domain.Business;
import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;
import com.populace.domain.enums.UserType;
import com.populace.repository.BusinessRepository;
import com.populace.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupController(BusinessRepository businessRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> response = new HashMap<>();

        // List all users
        var allUsers = userRepository.findAll();
        response.put("totalUsers", allUsers.size());
        response.put("users", allUsers.stream().map(u -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("email", u.getEmail());
            userMap.put("active", u.isActive());
            userMap.put("deleted", u.isDeleted());
            userMap.put("deletedAt", u.getDeletedAt());
            userMap.put("businessId", u.getBusinessId());
            userMap.put("passwordHashPrefix", u.getPasswordHash() != null ? u.getPasswordHash().substring(0, 20) + "..." : "NULL");
            return userMap;
        }).toList());

        // List all businesses
        var allBusinesses = businessRepository.findAll();
        response.put("totalBusinesses", allBusinesses.size());
        response.put("businesses", allBusinesses.stream().map(b -> Map.of(
            "id", b.getId(),
            "name", b.getName(),
            "email", b.getEmail()
        )).toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-login")
    public ResponseEntity<Map<String, Object>> testLogin() {
        Map<String, Object> response = new HashMap<>();
        String testEmail = "admin@populace.com";
        String testPassword = "Test@123";

        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(testEmail);

        if (userOpt.isEmpty()) {
            response.put("step1_userFound", false);
            response.put("message", "User not found with email: " + testEmail);
            return ResponseEntity.ok(response);
        }

        User user = userOpt.get();
        response.put("step1_userFound", true);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("isActive", user.isActive());
        response.put("isDeleted", user.isDeleted());
        response.put("deletedAt", user.getDeletedAt());

        boolean passwordMatches = passwordEncoder.matches(testPassword, user.getPasswordHash());
        response.put("step2_passwordMatches", passwordMatches);
        response.put("storedHashPrefix", user.getPasswordHash().substring(0, 30) + "...");

        // Generate a new hash for comparison
        String newHash = passwordEncoder.encode(testPassword);
        response.put("newHashForTest@123", newHash);

        if (!user.isActive()) {
            response.put("loginWouldFail", "Account is inactive");
        } else if (!passwordMatches) {
            response.put("loginWouldFail", "Password doesn't match");
        } else {
            response.put("loginShouldWork", true);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if admin already exists
            Optional<User> existingUser = userRepository.findByEmailAndDeletedAtIsNull("admin@populace.com");

            if (existingUser.isPresent()) {
                // Update password
                User user = existingUser.get();
                user.setPasswordHash(passwordEncoder.encode("Test@123"));
                user.setActive(true);
                user.setPermissionLevel(PermissionLevel.ADMIN);
                userRepository.save(user);

                response.put("status", "UPDATED");
                response.put("message", "Admin password updated");
                response.put("email", "admin@populace.com");
                response.put("password", "Test@123");
                return ResponseEntity.ok(response);
            }

            // Create business
            Business business = new Business();
            business.setName("Populace Restaurant Group");
            business.setEmail("setup-business@populace.com");
            business.setPhone("040-12345678");
            business.setBusinessCode("SETUP" + System.currentTimeMillis() % 10000);
            business.setToleranceOverPercentage(new BigDecimal("3"));
            business.setToleranceUnderPercentage(new BigDecimal("10"));
            business = businessRepository.save(business);

            // Create admin user
            User admin = new User();
            admin.setBusiness(business);
            admin.setEmail("admin@populace.com");
            admin.setPasswordHash(passwordEncoder.encode("Test@123"));
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setPhone("040-00000001");
            admin.setUserType(UserType.admin);
            admin.setPermissionLevel(PermissionLevel.ADMIN);
            admin.setActive(true);
            admin.setEmailVerifiedAt(Instant.now());
            userRepository.save(admin);

            response.put("status", "CREATED");
            response.put("message", "Admin user created successfully");
            response.put("email", "admin@populace.com");
            response.put("password", "Test@123");
            response.put("businessId", business.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
