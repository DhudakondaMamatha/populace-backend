package com.populace.user.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.permission.PermissionGuard;
import com.populace.user.dto.UpdatePermissionRequest;
import com.populace.user.dto.UserListDto;
import com.populace.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementService userService;
    private final PermissionGuard permissionGuard;

    public UserController(
            UserManagementService userService,
            PermissionGuard permissionGuard) {
        this.userService = userService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ResponseEntity<List<UserListDto>> listUsers(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionGuard.requireAdmin(principal);
        List<UserListDto> users = userService.listUsers(principal.getBusinessId());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserListDto> getUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionGuard.requireAdmin(principal);
        UserListDto user = userService.getUser(principal.getBusinessId(), id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/permission")
    public ResponseEntity<UserListDto> updatePermission(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        permissionGuard.requireAdmin(principal);
        UserListDto updated = userService.updatePermission(
            principal.getBusinessId(), id, request.permissionLevel());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionGuard.requireAdmin(principal);
        userService.deactivateUser(principal.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserListDto> activateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionGuard.requireAdmin(principal);
        UserListDto activated = userService.activateUser(principal.getBusinessId(), id);
        return ResponseEntity.ok(activated);
    }
}
