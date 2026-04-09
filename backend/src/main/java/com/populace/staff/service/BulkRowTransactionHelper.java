package com.populace.staff.service;

import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes each bulk upload row creation in its own transaction.
 * This prevents a single row failure from poisoning the outer transaction
 * (which would cause an UnexpectedRollbackException).
 */
@Component
public class BulkRowTransactionHelper {

    private final UnifiedStaffProvisioningService provisioningService;

    public BulkRowTransactionHelper(UnifiedStaffProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StaffDto createStaffInNewTransaction(Long businessId, UnifiedStaffCreateRequest request) {
        return provisioningService.createStaffWithFullProvisioning(businessId, request);
    }
}
