package com.populace.business.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.business.dto.BusinessSettingsDto;
import com.populace.business.dto.BusinessSettingsUpdateRequest;
import com.populace.business.service.BusinessSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/settings")
public class BusinessSettingsController {

    private final BusinessSettingsService businessSettingsService;

    public BusinessSettingsController(BusinessSettingsService businessSettingsService) {
        this.businessSettingsService = businessSettingsService;
    }

    @GetMapping
    public ResponseEntity<BusinessSettingsDto> getSettings(
            @AuthenticationPrincipal UserPrincipal user) {
        BusinessSettingsDto settings = businessSettingsService.getSettings(user.getBusinessId());
        return ResponseEntity.ok(settings);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<BusinessSettingsDto> updateSettings(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody BusinessSettingsUpdateRequest request) {
        BusinessSettingsDto settings = businessSettingsService.updateSettings(user.getBusinessId(), request);
        return ResponseEntity.ok(settings);
    }
}
