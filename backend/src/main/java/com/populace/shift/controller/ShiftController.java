package com.populace.shift.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.domain.enums.ShiftStatus;
import com.populace.shift.dto.BulkShiftCreateRequest;
import com.populace.shift.dto.BulkShiftCreateResponse;
import com.populace.shift.dto.BulkShiftUpdateRequest;
import com.populace.shift.dto.BulkShiftUpdateResponse;
import com.populace.shift.dto.ShiftCreateRequest;
import com.populace.shift.dto.ShiftDto;
import com.populace.shift.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private static final Logger log = LoggerFactory.getLogger(ShiftController.class);
    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping
    public ResponseEntity<List<ShiftDto>> listShifts(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate endDate,
            @RequestParam(required = false) List<Long> siteIds,
            @RequestParam(required = false) List<Long> roleIds,
            @RequestParam(required = false) List<ShiftStatus> statuses,
            @RequestParam(required = false, defaultValue = "false") Boolean excludeCancelled) {
        List<ShiftDto> shifts = shiftService.listShifts(
            user.getBusinessId(), startDate, endDate, siteIds, roleIds, statuses, excludeCancelled);
        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/unfilled")
    public ResponseEntity<List<ShiftDto>> listUnfilledShifts(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate endDate) {
        List<ShiftDto> shifts = shiftService.listUnfilledShifts(
            user.getBusinessId(), startDate, endDate);
        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftDto> getShift(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        ShiftDto shift = shiftService.getShiftById(user.getBusinessId(), id);
        return ResponseEntity.ok(shift);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ShiftDto> createShift(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ShiftCreateRequest request) {
        ShiftDto shift = shiftService.createShift(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(shift);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk")
    public ResponseEntity<BulkShiftCreateResponse> createBulkShifts(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BulkShiftCreateRequest request) {
        log.info("Bulk shift create - startTime: {}, endTime: {}",
            request.startTime(), request.endTime());
        BulkShiftCreateResponse response = shiftService.createBulkShifts(
            user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk-update")
    public ResponseEntity<BulkShiftUpdateResponse> bulkUpdateShifts(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BulkShiftUpdateRequest request) {
        BulkShiftUpdateResponse response = shiftService.bulkUpdateShifts(user.getBusinessId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ShiftDto> updateShift(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ShiftCreateRequest request) {
        ShiftDto shift = shiftService.updateShift(user.getBusinessId(), id, request);
        return ResponseEntity.ok(shift);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        shiftService.cancelShift(user.getBusinessId(), id);
        return ResponseEntity.noContent().build();
    }
}
