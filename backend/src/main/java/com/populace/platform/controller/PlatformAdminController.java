package com.populace.platform.controller;

import com.populace.auth.dto.UserResponse;
import com.populace.auth.service.UserPrincipal;
import com.populace.platform.dto.*;
import com.populace.platform.service.PlatformAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    public PlatformAdminController(PlatformAdminService platformAdminService) {
        this.platformAdminService = platformAdminService;
    }

    @GetMapping("/businesses")
    public ResponseEntity<List<BusinessSummaryDto>> listBusinesses() {
        return ResponseEntity.ok(platformAdminService.listBusinesses());
    }

    @GetMapping("/businesses/{id}")
    public ResponseEntity<BusinessDetailDto> getBusinessDetail(@PathVariable Long id) {
        return ResponseEntity.ok(platformAdminService.getBusinessDetail(id));
    }

    @PutMapping("/businesses/{id}/subscription")
    public ResponseEntity<BusinessDetailDto> updateSubscription(
            @PathVariable Long id,
            @RequestBody SubscriptionUpdateRequest request) {
        return ResponseEntity.ok(platformAdminService.updateSubscription(id, request));
    }

    @PutMapping("/businesses/{id}/status")
    public ResponseEntity<BusinessDetailDto> updateBusinessStatus(
            @PathVariable Long id,
            @RequestBody BusinessStatusRequest request) {
        return ResponseEntity.ok(platformAdminService.updateBusinessStatus(id, request));
    }

    @GetMapping("/businesses/{id}/users")
    public ResponseEntity<List<UserResponse>> getBusinessUsers(@PathVariable Long id) {
        return ResponseEntity.ok(platformAdminService.getBusinessUsers(id));
    }

    @PostMapping("/impersonate/{businessId}")
    public ResponseEntity<ImpersonationResponse> impersonate(
            @PathVariable Long businessId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(platformAdminService.impersonate(principal.getId(), businessId));
    }

    @GetMapping("/analytics")
    public ResponseEntity<PlatformAnalyticsDto> getAnalytics() {
        return ResponseEntity.ok(platformAdminService.getAnalytics());
    }
}
