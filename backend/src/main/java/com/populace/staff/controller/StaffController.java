package com.populace.staff.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.permission.PermissionGuard;
import com.populace.schedule.dto.WeeklySummaryDto;
import com.populace.schedule.service.ScheduleService;
import com.populace.staff.contract.StaffFieldContract;
import com.populace.staff.dto.BulkUploadResponse;
import com.populace.staff.dto.StaffAssignmentRequest;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.StaffFieldContractDto;
import com.populace.staff.dto.StaffUpdateRequest;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import com.populace.staff.dto.WorkParametersDto;
import com.populace.staff.dto.WorkParametersUpdateRequest;
import com.populace.staff.dto.LeaveSummaryDto;
import com.populace.staff.dto.StaffRoleDto;
import com.populace.staff.dto.StaffRoleUpdateRequest;
import com.populace.staff.service.BulkStaffUploadService;
import com.populace.staff.service.StaffService;
import com.populace.staff.service.StaffWorkParametersService;
import com.populace.staff.service.LeaveSummaryService;
import com.populace.staff.service.StaffRoleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;
    private final ScheduleService scheduleService;
    private final StaffWorkParametersService workParametersService;
    private final LeaveSummaryService leaveSummaryService;
    private final BulkStaffUploadService bulkUploadService;
    private final StaffRoleService staffRoleService;
    private final PermissionGuard permissionGuard;

    public StaffController(StaffService staffService,
                           ScheduleService scheduleService,
                           StaffWorkParametersService workParametersService,
                           LeaveSummaryService leaveSummaryService,
                           BulkStaffUploadService bulkUploadService,
                           StaffRoleService staffRoleService,
                           PermissionGuard permissionGuard) {
        this.staffService = staffService;
        this.scheduleService = scheduleService;
        this.workParametersService = workParametersService;
        this.leaveSummaryService = leaveSummaryService;
        this.bulkUploadService = bulkUploadService;
        this.staffRoleService = staffRoleService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ResponseEntity<List<StaffDto>> listStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String search) {
        List<StaffDto> staff = staffService.listStaff(
            user.getBusinessId(), status, employmentType, roleId, siteId, search);
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        StaffDto staff = staffService.getStaffById(user.getBusinessId(), id);
        return ResponseEntity.ok(staff);
    }

    @PostMapping
    public ResponseEntity<StaffDto> createStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UnifiedStaffCreateRequest request) {
        permissionGuard.requireStaffManagement(user);
        StaffDto staff = staffService.createStaffWithFullProvisioning(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(staff);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDto> updateStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody StaffUpdateRequest request) {
        permissionGuard.requireStaffManagement(user);
        StaffDto staff = staffService.updateStaff(user.getBusinessId(), id, request);
        return ResponseEntity.ok(staff);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        permissionGuard.requireStaffManagement(user);
        staffService.terminateStaff(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<StaffDto> assignRoles(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody StaffAssignmentRequest request) {
        permissionGuard.requireStaffManagement(user);
        StaffDto staff = staffService.assignRoles(user.getBusinessId(), id, request.getIds(), request.getPrimaryIds());
        return ResponseEntity.ok(staff);
    }

    @PutMapping("/{id}/sites")
    public ResponseEntity<StaffDto> assignSites(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody StaffAssignmentRequest request) {
        permissionGuard.requireStaffManagement(user);
        StaffDto staff = staffService.assignSites(user.getBusinessId(), id, request.getIds());
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/{id}/staff-roles")
    public ResponseEntity<List<StaffRoleDto>> getStaffRoles(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        List<StaffRoleDto> roles = staffRoleService.getStaffRoles(user.getBusinessId(), id);
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/{staffId}/staff-roles/{roleId}/break-rules")
    public ResponseEntity<StaffRoleDto> updateBreakRules(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId,
            @PathVariable Long roleId,
            @RequestBody StaffRoleUpdateRequest request) {
        permissionGuard.requireStaffManagement(user);
        StaffRoleDto updated = staffRoleService.updateBreakOverride(
            user.getBusinessId(), staffId, roleId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{staffId}/staff-roles/{roleId}/break-rules")
    public ResponseEntity<Void> clearBreakRules(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId,
            @PathVariable Long roleId) {
        permissionGuard.requireStaffManagement(user);
        staffRoleService.clearBreakOverride(user.getBusinessId(), staffId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/weekly-summary")
    public ResponseEntity<WeeklySummaryDto> getWeeklySummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WeeklySummaryDto summary = scheduleService.getStaffWeeklySummary(
            user.getBusinessId(), id, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/competence-levels")
    public ResponseEntity<List<String>> getCompetenceLevels(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        List<String> levels = staffService.getCompetenceLevels(user.getBusinessId(), id);
        return ResponseEntity.ok(levels);
    }

    @PutMapping("/{id}/competence-levels")
    public ResponseEntity<List<String>> updateCompetenceLevels(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody List<String> levels) {
        permissionGuard.requireStaffManagement(user);
        List<String> updated = staffService.updateCompetenceLevels(user.getBusinessId(), id, levels);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/work-parameters")
    public ResponseEntity<WorkParametersDto> getWorkParameters(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        WorkParametersDto params = workParametersService.getWorkParameters(user.getBusinessId(), id);
        return ResponseEntity.ok(params);
    }

    @PutMapping("/{id}/work-parameters")
    public ResponseEntity<WorkParametersDto> updateWorkParameters(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody WorkParametersUpdateRequest request) {
        permissionGuard.requireStaffManagement(user);
        WorkParametersDto updated = workParametersService.updateWorkParameters(user.getBusinessId(), id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/work-parameters")
    public ResponseEntity<Void> deleteWorkParameters(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        permissionGuard.requireStaffManagement(user);
        workParametersService.deleteWorkParameters(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/leave-summary")
    public ResponseEntity<LeaveSummaryDto> getLeaveSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        LeaveSummaryDto summary;
        if (year != null) {
            summary = leaveSummaryService.getLeaveSummary(user.getBusinessId(), id, year);
        } else {
            summary = leaveSummaryService.getCurrentLeaveSummary(user.getBusinessId(), id);
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/configuration-contract")
    public ResponseEntity<List<StaffFieldContractDto>> getConfigurationContract() {
        List<StaffFieldContractDto> contract = java.util.Arrays.stream(StaffFieldContract.values())
                .map(StaffFieldContractDto::fromContract)
                .toList();
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/bulk-upload/sample")
    public ResponseEntity<byte[]> downloadSampleCsv(@AuthenticationPrincipal UserPrincipal user) {
        permissionGuard.requireStaffManagement(user);
        String csvContent = bulkUploadService.generateSampleCsv();
        byte[] csvBytes = csvContent.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "staff-upload-sample.csv");
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> uploadStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        permissionGuard.requireStaffManagement(user);
        BulkUploadResponse response = bulkUploadService.parseAndCreateStaffFromCsv(user.getBusinessId(), file);

        if (response.success()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
