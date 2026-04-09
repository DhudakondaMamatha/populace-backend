package com.populace.site.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.site.dto.BulkOperatingHoursRequest;
import com.populace.site.dto.OperatingHoursDto;
import com.populace.site.dto.OperatingHoursRequest;
import com.populace.site.service.SiteOperatingHoursService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites/{siteId}/operating-hours")
public class SiteOperatingHoursController {

    private final SiteOperatingHoursService operatingHoursService;

    public SiteOperatingHoursController(SiteOperatingHoursService operatingHoursService) {
        this.operatingHoursService = operatingHoursService;
    }

    @GetMapping
    public ResponseEntity<List<OperatingHoursDto>> getOperatingHours(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long siteId) {
        List<OperatingHoursDto> hours = operatingHoursService.getOperatingHours(user.getBusinessId(), siteId);
        return ResponseEntity.ok(hours);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<List<OperatingHoursDto>> setBulkOperatingHours(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long siteId,
            @Valid @RequestBody BulkOperatingHoursRequest request) {
        List<OperatingHoursDto> hours = operatingHoursService.setBulkOperatingHours(user.getBusinessId(), siteId, request);
        return ResponseEntity.ok(hours);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{dayOfWeek}")
    public ResponseEntity<OperatingHoursDto> setOperatingHours(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long siteId,
            @PathVariable Integer dayOfWeek,
            @Valid @RequestBody OperatingHoursRequest request) {
        // Ensure path variable matches request body
        OperatingHoursRequest updatedRequest = new OperatingHoursRequest(
            dayOfWeek, request.isClosed(), request.openTime(), request.closeTime()
        );
        OperatingHoursDto hours = operatingHoursService.setOperatingHours(user.getBusinessId(), siteId, updatedRequest);
        return ResponseEntity.ok(hours);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{dayOfWeek}")
    public ResponseEntity<Void> deleteOperatingHours(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long siteId,
            @PathVariable Integer dayOfWeek) {
        operatingHoursService.deleteOperatingHours(user.getBusinessId(), siteId, dayOfWeek);
        return ResponseEntity.noContent().build();
    }
}
