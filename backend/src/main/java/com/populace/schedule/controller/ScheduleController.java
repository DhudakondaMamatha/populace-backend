package com.populace.schedule.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.onboarding.interceptor.RequiresOnboardingComplete;
import com.populace.schedule.dto.ScheduleFilterParams;
import com.populace.schedule.dto.StaffScheduleDto;
import com.populace.schedule.dto.WeeklySummaryDto;
import com.populace.schedule.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Schedule API controller.
 * Accessible by ALL authenticated users (ADMIN, MANAGER, STAFF).
 * NO DATABASE CHANGES REQUIRED - filtering is done in service layer.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiresOnboardingComplete
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Get schedule data with optional filters.
     * All filter parameters are optional - missing parameters mean "no filter".
     */
    @GetMapping
    public ResponseEntity<List<StaffScheduleDto>> getSchedule(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String resourceName) {

        ScheduleFilterParams filters = new ScheduleFilterParams(siteId, roleId, resourceName);

        List<StaffScheduleDto> schedules = scheduleService.getScheduleData(
            user.getBusinessId(), startDate, endDate, filters);

        return ResponseEntity.ok(schedules);
    }
}
