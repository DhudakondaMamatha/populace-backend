package com.populace.user.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;
import com.populace.repository.UserRepository;
import com.populace.user.dto.UserListDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserListDto> listUsers(Long businessId) {
        List<User> users = userRepository.findByBusiness_IdAndDeletedAtIsNull(businessId);
        return users.stream()
            .map(UserListDto::from)
            .toList();
    }

    public UserListDto getUser(Long businessId, Long userId) {
        User user = findUserInBusiness(businessId, userId);
        return UserListDto.from(user);
    }

    @Transactional
    public UserListDto updatePermission(Long businessId, Long userId, PermissionLevel permissionLevel) {
        User user = findUserInBusiness(businessId, userId);
        user.setPermissionLevel(permissionLevel);
        User saved = userRepository.save(user);
        return UserListDto.from(saved);
    }

    @Transactional
    public void deactivateUser(Long businessId, Long userId) {
        User user = findUserInBusiness(businessId, userId);

        if (user.getPermissionLevel() == PermissionLevel.ADMIN) {
            long adminCount = countAdminsInBusiness(businessId);
            if (adminCount <= 1) {
                throw new ValidationException("Cannot deactivate the only admin user");
            }
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserListDto activateUser(Long businessId, Long userId) {
        User user = findUserInBusiness(businessId, userId);
        user.setActive(true);
        User saved = userRepository.save(user);
        return UserListDto.from(saved);
    }

    private User findUserInBusiness(Long businessId, Long userId) {
        return userRepository.findById(userId)
            .filter(u -> u.getBusinessId().equals(businessId))
            .filter(u -> u.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private long countAdminsInBusiness(Long businessId) {
        return userRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).stream()
            .filter(u -> u.getPermissionLevel() == PermissionLevel.ADMIN)
            .filter(User::isActive)
            .count();
    }
}
