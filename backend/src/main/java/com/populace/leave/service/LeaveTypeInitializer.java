package com.populace.leave.service;

import com.populace.domain.Business;
import com.populace.domain.LeaveType;
import com.populace.domain.enums.AccrualPeriod;
import com.populace.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Initializes default leave types for a new business.
 */
@Service
public class LeaveTypeInitializer {

    private static final Logger log = LoggerFactory.getLogger(LeaveTypeInitializer.class);

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeInitializer(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional
    public void initializeDefaultLeaveTypes(Business business) {
        List<LeaveType> existing = leaveTypeRepository.findByBusiness_IdAndActiveTrue(business.getId());
        if (!existing.isEmpty()) {
            log.debug("Business {} already has {} leave types", business.getId(), existing.size());
            return;
        }

        log.info("Initializing default leave types for business {}", business.getId());

        // Annual Leave
        createLeaveType(business, "Annual Leave", "AL", true,
            new BigDecimal("1.25"), AccrualPeriod.monthly, new BigDecimal("30"),
            new BigDecimal("5"), true, 7, "#4CAF50");

        // Sick Leave
        createLeaveType(business, "Sick Leave", "SL", true,
            new BigDecimal("0.5"), AccrualPeriod.monthly, new BigDecimal("12"),
            null, false, 0, "#F44336");

        // Casual Leave
        createLeaveType(business, "Casual Leave", "CL", true,
            new BigDecimal("0.5"), AccrualPeriod.monthly, new BigDecimal("6"),
            null, true, 1, "#2196F3");

        // Unpaid Leave
        createLeaveType(business, "Unpaid Leave", "UL", false,
            null, null, null,
            null, true, 3, "#9E9E9E");

        log.info("Created 4 default leave types for business {}", business.getId());
    }

    private void createLeaveType(Business business, String name, String code, boolean paid,
                                  BigDecimal accrualRate, AccrualPeriod accrualPeriod,
                                  BigDecimal maxBalance, BigDecimal carryOverLimit,
                                  boolean requiresApproval, Integer minNoticeDays, String color) {
        LeaveType lt = new LeaveType();
        lt.setBusiness(business);
        lt.setName(name);
        lt.setCode(code);
        lt.setPaid(paid);
        lt.setAccrualRate(accrualRate);
        lt.setAccrualPeriod(accrualPeriod != null ? accrualPeriod : AccrualPeriod.monthly);
        lt.setMaxBalance(maxBalance);
        lt.setCarryOverLimit(carryOverLimit);
        lt.setRequiresApproval(requiresApproval);
        lt.setMinNoticeDays(minNoticeDays);
        lt.setColor(color);
        lt.setActive(true);
        leaveTypeRepository.save(lt);
    }
}
