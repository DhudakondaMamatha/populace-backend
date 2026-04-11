package com.populace.onboarding.controller;

import com.populace.auth.util.SecurityUtils;
import com.populace.onboarding.dto.OnboardingStatusResponse;
import com.populace.onboarding.service.OnboardingEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingEvaluationService evaluationService;

    public OnboardingController(OnboardingEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResponse> getStatus() {
        // Platform admins have no business context — onboarding is not applicable
        if (SecurityUtils.isPlatformAdmin()) {
            return ResponseEntity.ok(new OnboardingStatusResponse(true, null, 0, 0, List.of()));
        }

        Long businessId = SecurityUtils.getCurrentBusinessId();

        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }

        OnboardingStatusResponse status = evaluationService.evaluateAllStages(businessId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/refresh")
    public ResponseEntity<OnboardingStatusResponse> refreshStatus() {
        if (SecurityUtils.isPlatformAdmin()) {
            return ResponseEntity.ok(new OnboardingStatusResponse(true, null, 0, 0, List.of()));
        }

        Long businessId = SecurityUtils.getCurrentBusinessId();

        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }

        OnboardingStatusResponse status = evaluationService.evaluateAllStages(businessId);
        return ResponseEntity.ok(status);
    }
}
