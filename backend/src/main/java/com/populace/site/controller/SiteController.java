package com.populace.site.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.site.dto.SiteCreateRequest;
import com.populace.site.dto.SiteDto;
import com.populace.site.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public ResponseEntity<List<SiteDto>> listSites(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<SiteDto> sites = siteService.listSites(user.getBusinessId(), status, search);
        return ResponseEntity.ok(sites);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteDto> getSite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        SiteDto site = siteService.getSiteById(user.getBusinessId(), id);
        return ResponseEntity.ok(site);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SiteDto> createSite(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody SiteCreateRequest request) {
        SiteDto site = siteService.createSite(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(site);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SiteDto> updateSite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody SiteCreateRequest request) {
        SiteDto site = siteService.updateSite(user.getBusinessId(), id, request);
        return ResponseEntity.ok(site);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        siteService.deactivateSite(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }
}
