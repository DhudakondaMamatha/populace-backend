package com.populace.reporting.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.reporting.dto.StaffHoursReportDto;
import com.populace.reporting.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/staff-hours")
    public ResponseEntity<StaffHoursReportDto> getStaffHours(
            @AuthenticationPrincipal UserPrincipal user) {
        StaffHoursReportDto report = reportService.getStaffHoursSummary(user.getBusinessId());
        return ResponseEntity.ok(report);
    }
}
