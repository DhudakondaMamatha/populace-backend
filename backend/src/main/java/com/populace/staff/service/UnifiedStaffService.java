package com.populace.staff.service;

import com.populace.common.exception.OptimisticLockException;
import com.populace.compensation.service.StaffCompensationService;
import com.populace.repository.StaffMemberRepository;
import com.populace.staff.dto.BulkUpsertResponse;
import com.populace.staff.dto.RoleAssignment;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.StaffUpdateRequest;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import com.populace.staff.dto.UnifiedStaffUpsertRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified service for staff create/update operations.
 * Provides a single endpoint for both creating new staff and updating existing staff.
 *
 * <h2>Single Upsert</h2>
 * The {@link #upsertStaff} method handles both create and update in a single transaction.
 *
 * <h2>Bulk Upsert</h2>
 * The {@link #bulkUpsert} method processes each item in its own transaction,
 * so individual failures don't roll back the entire batch.
 */
@Service
public class UnifiedStaffService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedStaffService.class);

    private final UnifiedStaffProvisioningService provisioningService;
    private final StaffService staffService;
    private final StaffCompensationService compensationService;
    private final StaffMemberRepository staffRepository;
    private final UpsertTransactionHelper transactionHelper;

    public UnifiedStaffService(
            UnifiedStaffProvisioningService provisioningService,
            StaffService staffService,
            StaffCompensationService compensationService,
            StaffMemberRepository staffRepository,
            UpsertTransactionHelper transactionHelper) {
        this.provisioningService = provisioningService;
        this.staffService = staffService;
        this.compensationService = compensationService;
        this.staffRepository = staffRepository;
        this.transactionHelper = transactionHelper;
    }

    /**
     * Unified upsert: create if id is null, update if id is present.
     * This method runs in a single transaction.
     */
    @Transactional
    public StaffDto upsertStaff(Long businessId, UnifiedStaffUpsertRequest request) {
        if (request.isCreate()) {
            return createStaff(businessId, request);
        } else {
            return updateStaff(businessId, request);
        }
    }

    /**
     * Batch upsert with individual transactions.
     * NOT @Transactional - each item is processed independently via the helper.
     * This ensures that individual failures don't roll back the entire batch.
     */
    public BulkUpsertResponse bulkUpsert(Long businessId, List<UnifiedStaffUpsertRequest> requests) {
        log.info("Processing bulk upsert of {} staff for business {}", requests.size(), businessId);

        List<BulkUpsertResponse.UpsertResult> results = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            UnifiedStaffUpsertRequest request = requests.get(i);
            BulkUpsertResponse.UpsertResult result = transactionHelper.upsertInNewTransaction(
                businessId, request, i
            );
            results.add(result);
        }

        BulkUpsertResponse response = BulkUpsertResponse.withErrors(results);
        log.info("Bulk upsert completed: {} success, {} errors",
            response.successCount(), response.errorCount());

        return response;
    }

    private StaffDto createStaff(Long businessId, UnifiedStaffUpsertRequest request) {
        log.info("Creating staff: {} {}", request.firstName(), request.lastName());
        UnifiedStaffCreateRequest createRequest = toCreateRequest(request);
        return provisioningService.createStaffWithFullProvisioning(businessId, createRequest);
    }

    private StaffDto updateStaff(Long businessId, UnifiedStaffUpsertRequest request) {
        log.info("Updating staff {}", request.id());

        Long staffId = request.id();
        Long version = request.version();

        // Verify version for optimistic locking
        verifyVersion(staffId, version);

        // Update basic info
        StaffUpdateRequest updateRequest = toUpdateRequest(request);
        StaffDto result = staffService.updateStaff(businessId, staffId, updateRequest);

        // Sync role assignments
        if (request.hasRoleAssignments()) {
            syncRoleAssignments(businessId, staffId, request.roleAssignments());
        }

        // Sync site assignments
        if (request.hasSiteAssignments()) {
            syncSiteAssignments(businessId, staffId, request.siteIds());
        }

        // Update compensation
        if (request.hasCompensation()) {
            updateCompensation(staffId, request);
        }

        return staffService.getStaffById(businessId, staffId);
    }

    private void verifyVersion(Long staffId, Long expectedVersion) {
        if (expectedVersion == null) {
            return; // Skip version check if not provided
        }

        Long currentVersion = staffRepository.findVersionById(staffId);
        if (currentVersion != null && !currentVersion.equals(expectedVersion)) {
            throw new OptimisticLockException(
                "Staff record was modified by another user. Please refresh and try again."
            );
        }
    }

    private void syncRoleAssignments(Long businessId, Long staffId, List<RoleAssignment> assignments) {
        List<Long> roleIds = assignments.stream()
            .map(RoleAssignment::roleId)
            .toList();
        List<Long> primaryIds = assignments.stream()
            .filter(ra -> Boolean.TRUE.equals(ra.isPrimary()))
            .map(RoleAssignment::roleId)
            .toList();
        staffService.assignRoles(businessId, staffId, roleIds, primaryIds);
    }

    private void syncSiteAssignments(Long businessId, Long staffId, List<Long> siteIds) {
        staffService.assignSites(businessId, staffId, siteIds);
    }

    private void updateCompensation(Long staffId, UnifiedStaffUpsertRequest request) {
        compensationService.upsertCompensation(staffId, request.compensationType(),
            request.hourlyRate(), request.monthlySalary());
    }

    private UnifiedStaffCreateRequest toCreateRequest(UnifiedStaffUpsertRequest request) {
        return new UnifiedStaffCreateRequest(
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone(),
            request.employeeCode(),
            request.employmentType(),
            request.roleIds(),
            request.siteIds(),
            request.competenceLevels(),
            request.roleAssignments(),
            request.compensationType(),
            request.hourlyRate(),
            request.monthlySalary(),
            request.minHoursPerDay(),
            request.maxHoursPerDay(),
            request.minHoursPerMonth(),
            request.maxHoursPerMonth(),
            request.minDaysOffPerWeek(),
            request.maxSitesPerDay(),
            request.minHoursPerWeek(),
            request.maxHoursPerWeek(),
            request.mustGoOnLeaveAfterDays(),
            request.accruesOneDayLeaveAfterDays()
        );
    }

    private StaffUpdateRequest toUpdateRequest(UnifiedStaffUpsertRequest request) {
        return new StaffUpdateRequest(
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone(),
            request.employeeCode(),
            request.employmentType(),
            request.minHoursPerDay(),
            request.maxHoursPerDay(),
            request.minHoursPerWeek(),
            request.maxHoursPerWeek(),
            request.minHoursPerMonth(),
            request.maxHoursPerMonth(),
            request.minDaysOffPerWeek(),
            request.maxSitesPerDay(),
            request.mustGoOnLeaveAfterDays(),
            request.accruesOneDayLeaveAfterDays()
        );
    }
}
