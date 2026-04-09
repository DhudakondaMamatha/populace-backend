package com.populace.staff.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for displaying leave accrual summary for a staff member.
 */
public record LeaveSummaryDto(
    int year,
    BigDecimal totalHoursWorked,
    List<LeaveTypeBalance> leaveBalances
) {
    /**
     * Leave balance information per leave type.
     */
    public record LeaveTypeBalance(
        Long leaveTypeId,
        String leaveTypeName,
        String accrualRule,
        BigDecimal earned,
        BigDecimal used,
        BigDecimal balance,
        boolean isPaid
    ) {}
}
