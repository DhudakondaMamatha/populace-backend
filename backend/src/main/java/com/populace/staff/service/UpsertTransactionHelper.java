package com.populace.staff.service;

import com.populace.common.exception.OptimisticLockException;
import com.populace.common.exception.ValidationException;
import com.populace.compensation.service.StaffCompensationService;
import com.populace.domain.StaffMember;
import com.populace.repository.StaffMemberRepository;
import com.populace.staff.dto.BulkUpsertResponse;
import com.populace.staff.dto.RoleAssignment;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.StaffUpdateRequest;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import com.populace.staff.dto.UnifiedStaffUpsertRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Helper component to run each upsert operation in its own transaction.
 * Must be a separate bean for REQUIRES_NEW propagation to work correctly.
 *
 * This enables bulk upsert operations where individual failures don't
 * rollback the entire batch - each item succeeds or fails independently.
 */
@Component
public class UpsertTransactionHelper {

    private static final Logger log = LoggerFactory.getLogger(UpsertTransactionHelper.class);

    private final UnifiedStaffProvisioningService provisioningService;
    private final StaffService staffService;
    private final StaffCompensationService compensationService;
    private final StaffMemberRepository staffRepository;

    public UpsertTransactionHelper(
            UnifiedStaffProvisioningService provisioningService,
            StaffService staffService,
            StaffCompensationService compensationService,
            StaffMemberRepository staffRepository) {
        this.provisioningService = provisioningService;
        this.staffService = staffService;
        this.compensationService = compensationService;
        this.staffRepository = staffRepository;
    }

    /**
     * Process a single upsert request in its own transaction.
     * If this fails, only this item is rolled back - other items are unaffected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BulkUpsertResponse.UpsertResult upsertInNewTransaction(
            Long businessId,
            UnifiedStaffUpsertRequest request,
            int index) {

        try {
            StaffDto staff = processUpsert(businessId, request);
            return createSuccessResult(index, staff);
        } catch (ValidationException e) {
            log.warn("Validation failed at index {}: {}", index, e.getMessage());
            return createValidationErrorResult(index, e);
        } catch (OptimisticLockException e) {
            log.warn("Version conflict at index {}: {}", index, e.getMessage());
            return createVersionErrorResult(index, e);
        } catch (Exception e) {
            log.error("Unexpected error at index {}: {}", index, e.getMessage());
            return createUnexpectedErrorResult(index, e);
        }
    }

    private StaffDto processUpsert(Long businessId, UnifiedStaffUpsertRequest request) {
        if (request.isCreate()) {
            return createStaff(businessId, request);
        } else {
            return updateStaff(businessId, request);
        }
    }

    private StaffDto createStaff(Long businessId, UnifiedStaffUpsertRequest request) {
        UnifiedStaffCreateRequest createRequest = toCreateRequest(request);
        return provisioningService.createStaffWithFullProvisioning(businessId, createRequest);
    }

    private StaffDto updateStaff(Long businessId, UnifiedStaffUpsertRequest request) {
        Long staffId = request.id();

        // Verify version for optimistic locking
        verifyVersion(staffId, request.version());

        // Update basic info
        StaffUpdateRequest updateRequest = toUpdateRequest(request);
        StaffDto result = staffService.updateStaff(businessId, staffId, updateRequest);

        // Sync role assignments
        if (request.hasRoleAssignments()) {
            List<Long> roleIds = request.roleAssignments().stream()
                .map(RoleAssignment::roleId).toList();
            List<Long> primaryIds = request.roleAssignments().stream()
                .filter(ra -> Boolean.TRUE.equals(ra.isPrimary()))
                .map(RoleAssignment::roleId).toList();
            staffService.assignRoles(businessId, staffId, roleIds, primaryIds);
        }

        // Sync site assignments
        if (request.hasSiteAssignments()) {
            staffService.assignSites(businessId, staffId, request.siteIds());
        }

        // Update compensation
        if (request.hasCompensation()) {
            compensationService.upsertCompensation(staffId, request.compensationType(),
                request.hourlyRate(), request.monthlySalary());
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

    private BulkUpsertResponse.UpsertResult createSuccessResult(int index, StaffDto staff) {
        return new BulkUpsertResponse.UpsertResult(
            index, true, staff.id(),
            staff.firstName() + " " + staff.lastName(),
            List.of()
        );
    }

    private BulkUpsertResponse.UpsertResult createValidationErrorResult(int index, ValidationException e) {
        List<BulkUpsertResponse.ValidationError> errors = e.getFieldErrors().stream()
            .map(fe -> new BulkUpsertResponse.ValidationError(fe.field(), fe.message(), "VALIDATION_ERROR"))
            .toList();

        if (errors.isEmpty()) {
            errors = List.of(new BulkUpsertResponse.ValidationError(null, e.getMessage(), "VALIDATION_ERROR"));
        }

        return new BulkUpsertResponse.UpsertResult(index, false, null, null, errors);
    }

    private BulkUpsertResponse.UpsertResult createVersionErrorResult(int index, OptimisticLockException e) {
        return new BulkUpsertResponse.UpsertResult(
            index, false, null, null,
            List.of(new BulkUpsertResponse.ValidationError("version", e.getMessage(), "VERSION_MISMATCH"))
        );
    }

    private BulkUpsertResponse.UpsertResult createUnexpectedErrorResult(int index, Exception e) {
        return new BulkUpsertResponse.UpsertResult(
            index, false, null, null,
            List.of(new BulkUpsertResponse.ValidationError(null, e.getMessage(), "UNEXPECTED_ERROR"))
        );
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
