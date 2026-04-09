package com.populace.leave.controller;

import com.populace.auth.service.UserPrincipal;
import com.populace.domain.enums.LeaveRequestStatus;
import com.populace.leave.dto.LeaveRequestCreateDto;
import com.populace.leave.dto.LeaveRequestDto;
import com.populace.leave.dto.LeaveReviewDto;
import com.populace.leave.dto.LeaveTypeDto;
import com.populace.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/requests")
    public ResponseEntity<List<LeaveRequestDto>> listRequests(
            @AuthenticationPrincipal UserPrincipal user) {
        List<LeaveRequestDto> requests = leaveService.listRequests(user.getBusinessId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<LeaveRequestDto> getRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        LeaveRequestDto request = leaveService.getRequest(user.getBusinessId(), id);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/requests")
    public ResponseEntity<LeaveRequestDto> createRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody LeaveRequestCreateDto dto) {
        LeaveRequestDto request = leaveService.createRequest(user.getBusinessId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<LeaveRequestDto> approveRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody(required = false) LeaveReviewDto dto) {
        LeaveRequestDto request = leaveService.approveRequest(
            user.getBusinessId(), id, user.getId(), dto);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<LeaveRequestDto> rejectRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody(required = false) LeaveReviewDto dto) {
        LeaveRequestDto request = leaveService.rejectRequest(
            user.getBusinessId(), id, user.getId(), dto);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/requests/staff/{staffId}")
    public ResponseEntity<List<LeaveRequestDto>> listRequestsByStaff(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long staffId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year) {
        LeaveRequestStatus statusEnum = status != null ?
            LeaveRequestStatus.valueOf(status) : null;
        List<LeaveRequestDto> requests = leaveService.listRequestsByStaff(
            user.getBusinessId(), staffId, statusEnum, year);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/requests/{id}/cancel")
    public ResponseEntity<LeaveRequestDto> cancelRequest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        LeaveRequestDto request = leaveService.cancelRequest(
            user.getBusinessId(), id, user.getId());
        return ResponseEntity.ok(request);
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeDto>> listLeaveTypes(
            @AuthenticationPrincipal UserPrincipal user) {
        List<LeaveTypeDto> types = leaveService.listLeaveTypes(user.getBusinessId());
        return ResponseEntity.ok(types);
    }
}
