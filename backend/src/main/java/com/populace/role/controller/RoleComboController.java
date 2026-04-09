package com.populace.role.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.role.dto.RoleComboCreateRequest;
import com.populace.role.dto.RoleComboDto;
import com.populace.role.service.RoleComboService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-combos")
public class RoleComboController {

    private final RoleComboService roleComboService;

    public RoleComboController(RoleComboService roleComboService) {
        this.roleComboService = roleComboService;
    }

    @GetMapping
    public ResponseEntity<List<RoleComboDto>> listCombos(
            @AuthenticationPrincipal UserPrincipal user) {
        List<RoleComboDto> combos = roleComboService.listCombos(user.getBusinessId());
        return ResponseEntity.ok(combos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoleComboDto> createCombo(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody RoleComboCreateRequest request) {
        RoleComboDto combo = roleComboService.createCombo(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(combo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RoleComboDto> updateCombo(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody RoleComboCreateRequest request) {
        RoleComboDto combo = roleComboService.updateCombo(user.getBusinessId(), id, request);
        return ResponseEntity.ok(combo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCombo(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        roleComboService.deactivateCombo(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }
}
