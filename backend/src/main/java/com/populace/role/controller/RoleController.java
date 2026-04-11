package com.populace.role.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.role.dto.RoleCreateRequest;
import com.populace.role.dto.RoleDto;
import com.populace.role.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> listRoles(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<RoleDto> roles = roleService.listRoles(user.getBusinessId(), status, search);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRole(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        RoleDto role = roleService.getRoleById(user.getBusinessId(), id);
        return ResponseEntity.ok(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoleDto> createRole(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody RoleCreateRequest request) {
        RoleDto role = roleService.createRole(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody RoleCreateRequest request) {
        RoleDto role = roleService.updateRole(user.getBusinessId(), id, request);
        return ResponseEntity.ok(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        roleService.deactivateRole(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }
}
