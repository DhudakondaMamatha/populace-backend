package com.populace.allocation.controller;

import com.populace.allocation.dto.*;
import com.populace.allocation.service.AllocationEngine;
import com.populace.auth.util.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationEngine allocationEngine;

    public AllocationController(AllocationEngine allocationEngine) {
        this.allocationEngine = allocationEngine;
    }

    /**
     * Pre-check: returns a summary of what would happen if allocation is run.
     * GET /api/allocations/precheck?startDate=X&endDate=Y
     */
    @GetMapping("/precheck")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreCheckDto> preCheck(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        PreCheckDto result = allocationEngine.preCheck(businessId, startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * Run allocation for a date range.
     * POST /api/allocations/run
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationResultDto> runAllocation(@RequestBody AllocationRequest request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        AllocationResultDto result = allocationEngine.allocate(
                businessId,
                request.startDate(),
                request.endDate(),
                true);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk re-allocate for a date range (optionally forcing re-allocation even on filled shifts).
     * POST /api/allocations/reallocate-all
     */
    @PostMapping("/reallocate-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationResultDto> bulkReallocate(@RequestBody BulkReallocateRequest request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        AllocationResultDto result = allocationEngine.allocate(
                businessId,
                request.startDate(),
                request.endDate(),
                request.forceReallocate());
        return ResponseEntity.ok(result);
    }

    /**
     * List previous allocation runs.
     * GET /api/allocations/runs
     * Returns empty list (run history not yet persisted).
     */
    @GetMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Object>> getRuns() {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * Get a specific allocation run by ID.
     * GET /api/allocations/runs/{id}
     * Returns 404 (run history not yet persisted).
     */
    @GetMapping("/runs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getRun(@PathVariable String id) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Get eligible staff for a shift.
     * GET /api/allocations/shifts/{shiftId}/eligible-staff
     */
    @GetMapping("/shifts/{shiftId}/eligible-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EligibleStaffDto> getEligibleStaff(@PathVariable Long shiftId) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        EligibleStaffDto result = allocationEngine.getEligibleStaff(businessId, shiftId);
        return ResponseEntity.ok(result);
    }

    /**
     * Basic eligibility check endpoint.
     * POST /api/allocations/eligibility
     */
    @PostMapping("/eligibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> checkEligibility(@RequestBody(required = false) Object request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        // Basic endpoint — returns acknowledgement; detailed eligibility is via eligible-staff
        return ResponseEntity.ok(java.util.Map.of(
                "status", "ok",
                "message", "Use GET /api/allocations/shifts/{shiftId}/eligible-staff for detailed eligibility."));
    }

    /**
     * Manually allocate a staff member to a shift.
     * POST /api/allocations/shifts/{shiftId}/allocations
     */
    @PostMapping("/shifts/{shiftId}/allocations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManualAllocationResponse> createManualAllocation(
            @PathVariable Long shiftId,
            @RequestBody ManualAllocationRequest request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        ManualAllocationResponse result = allocationEngine.createManualAllocation(businessId, shiftId, request);
        if (!result.success()) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Remove an allocation (time block) from a shift. Only manual allocations can be removed.
     * DELETE /api/allocations/shifts/{shiftId}/allocations/{blockId}
     */
    @DeleteMapping("/shifts/{shiftId}/allocations/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RemoveAllocationResponse> removeAllocation(
            @PathVariable Long shiftId,
            @PathVariable Long blockId) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        RemoveAllocationResponse result = allocationEngine.removeAllocation(businessId, shiftId, blockId);
        if (!result.success()) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Get all allocations (time blocks) for a shift.
     * GET /api/allocations/shifts/{shiftId}/allocations
     */
    @GetMapping("/shifts/{shiftId}/allocations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftAllocationsDto> getShiftAllocations(@PathVariable Long shiftId) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        ShiftAllocationsDto result = allocationEngine.getShiftAllocations(businessId, shiftId);
        return ResponseEntity.ok(result);
    }

    /**
     * Re-allocate a single shift (clears auto-allocations and re-runs).
     * POST /api/allocations/shifts/{shiftId}/reallocate
     */
    @PostMapping("/shifts/{shiftId}/reallocate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReallocateShiftResponse> reallocateShift(@PathVariable Long shiftId) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        if (businessId == null) {
            return ResponseEntity.badRequest().build();
        }
        ReallocateShiftResponse result = allocationEngine.reallocateShift(businessId, shiftId);
        return ResponseEntity.ok(result);
    }
}
