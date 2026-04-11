package com.populace.shift.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.shift.dto.*;
import com.populace.shift.service.ShiftTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shift-templates")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<ScheduleTemplateDto> getForSite(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long siteId) {
        ScheduleTemplateDto template = shiftTemplateService.getOrCreateForSite(
            user.getBusinessId(), siteId);
        return ResponseEntity.ok(template);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ScheduleTemplateDto> saveTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ScheduleTemplateSaveRequest request) {
        ScheduleTemplateDto template = shiftTemplateService.saveTemplate(
            user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/generate")
    public ResponseEntity<GenerateShiftsFromTemplateResponse> generateShifts(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody GenerateShiftsFromTemplateRequest request) {
        GenerateShiftsFromTemplateResponse response = shiftTemplateService.generateShifts(
            user.getBusinessId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        shiftTemplateService.deleteTemplate(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }
}
