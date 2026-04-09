package com.populace.dashboard.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.dashboard.dto.DashboardSummaryDto;
import com.populace.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal user) {
        DashboardSummaryDto summary = dashboardService.getDashboardSummary(user.getBusinessId());
        return ResponseEntity.ok(summary);
    }
}
