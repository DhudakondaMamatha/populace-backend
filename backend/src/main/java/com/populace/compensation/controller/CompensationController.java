package com.populace.compensation.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.dto.CompensationUpdateRequest;
import com.populace.compensation.service.StaffCompensationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for staff compensation management.
 * Admins can create/update compensation records.
 * All authenticated users can read compensation data.
 */
@RestController
@RequestMapping("/api")
public class CompensationController {

    private final StaffCompensationService compensationService;

    public CompensationController(StaffCompensationService compensationService) {
        this.compensationService = compensationService;
    }

    /**
     * Get compensation history for a staff member.
     * Available to all authenticated users.
     */
    @GetMapping("/staff/{staffId}/compensation")
    public ResponseEntity<List<CompensationDto>> getCompensationHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId) {
        List<CompensationDto> history = compensationService.getCompensationHistory(staffId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get the currently active compensation for a staff member.
     * Available to all authenticated users.
     */
    @GetMapping("/staff/{staffId}/compensation/current")
    public ResponseEntity<CompensationDto> getCurrentCompensation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId) {
        return compensationService.getCurrentCompensation(staffId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Create a new compensation record for a staff member.
     * Requires ADMIN role.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/staff/{staffId}/compensation")
    public ResponseEntity<CompensationDto> createCompensation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId,
            @RequestBody CompensationCreateRequest request) {
        CompensationDto created = compensationService.createCompensation(staffId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing compensation record.
     * Requires ADMIN role.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/compensation/{id}")
    public ResponseEntity<CompensationDto> updateCompensation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody CompensationUpdateRequest request) {
        CompensationDto updated = compensationService.updateCompensation(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * End a compensation record by setting its effective end date.
     * Requires ADMIN role.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/compensation/{id}")
    public ResponseEntity<Void> endCompensation(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody EndCompensationRequest request) {
        compensationService.endCompensation(id, request.endDate());
        return ResponseEntity.noContent().build();
    }

    /**
     * Request body for ending a compensation record.
     */
    public record EndCompensationRequest(LocalDate endDate) {}
}
