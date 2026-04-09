package com.populace.staff.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.LeaveType;
import com.populace.domain.StaffLeaveBalance;
import com.populace.domain.StaffMember;
import com.populace.domain.enums.AccrualPeriod;
import com.populace.repository.LeaveTypeRepository;
import com.populace.repository.StaffLeaveBalanceRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.TimeBlockRepository;
import com.populace.staff.dto.LeaveSummaryDto;
import com.populace.staff.dto.LeaveSummaryDto.LeaveTypeBalance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaveSummaryService {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final StaffMemberRepository staffRepository;
    private final StaffLeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final TimeBlockRepository timeBlockRepository;

    public LeaveSummaryService(StaffMemberRepository staffRepository,
                               StaffLeaveBalanceRepository leaveBalanceRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               TimeBlockRepository timeBlockRepository) {
        this.staffRepository = staffRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.timeBlockRepository = timeBlockRepository;
    }

    /**
     * Get leave summary for a staff member for a specific year.
     */
    @Transactional(readOnly = true)
    public LeaveSummaryDto getLeaveSummary(Long businessId, Long staffId, int year) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        // Get total hours worked for the year from TimeBlocks
        Integer totalMinutesWorked = timeBlockRepository.sumWorkMinutesByStaffAndYear(staffId, year);
        BigDecimal totalHoursWorked = convertMinutesToHours(totalMinutesWorked);

        // Get all active leave types for the business
        List<LeaveType> leaveTypes = leaveTypeRepository.findByBusiness_IdAndActiveTrue(businessId);

        // Get leave balances for the staff member for this year
        List<StaffLeaveBalance> balances = leaveBalanceRepository.findByStaffIdAndYear(staffId, year);
        Map<Long, StaffLeaveBalance> balanceMap = balances.stream()
            .collect(Collectors.toMap(b -> b.getLeaveType().getId(), Function.identity()));

        // Build leave balance summaries
        List<LeaveTypeBalance> leaveBalances = new ArrayList<>();
        for (LeaveType leaveType : leaveTypes) {
            StaffLeaveBalance balance = balanceMap.get(leaveType.getId());

            BigDecimal earned = balance != null ? balance.getAccrued() : BigDecimal.ZERO;
            BigDecimal used = balance != null ? balance.getUsed() : BigDecimal.ZERO;
            BigDecimal currentBalance = balance != null ? balance.getCurrentBalance() : BigDecimal.ZERO;

            String accrualRule = formatAccrualRule(leaveType);

            leaveBalances.add(new LeaveTypeBalance(
                leaveType.getId(),
                leaveType.getName(),
                accrualRule,
                earned,
                used,
                currentBalance,
                leaveType.isPaid()
            ));
        }

        return new LeaveSummaryDto(year, totalHoursWorked, leaveBalances);
    }

    /**
     * Get leave summary for the current year.
     */
    @Transactional(readOnly = true)
    public LeaveSummaryDto getCurrentLeaveSummary(Long businessId, Long staffId) {
        int currentYear = LocalDate.now().getYear();
        return getLeaveSummary(businessId, staffId, currentYear);
    }

    private BigDecimal convertMinutesToHours(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    /**
     * Format the accrual rule for display.
     * Examples:
     * - "Earn 2 days per month"
     * - "Earn 1 day per year"
     */
    private String formatAccrualRule(LeaveType leaveType) {
        if (leaveType.getAccrualRate() == null || leaveType.getAccrualPeriod() == null) {
            return "No automatic accrual";
        }

        BigDecimal rate = leaveType.getAccrualRate();
        AccrualPeriod period = leaveType.getAccrualPeriod();

        String rateStr = rate.stripTrailingZeros().toPlainString();
        String dayWord = rate.compareTo(BigDecimal.ONE) == 0 ? "day" : "days";
        String periodStr = formatPeriod(period);

        return String.format("Earn %s %s %s", rateStr, dayWord, periodStr);
    }

    private String formatPeriod(AccrualPeriod period) {
        return switch (period) {
            case daily -> "per day";
            case weekly -> "per week";
            case monthly -> "per month";
            case yearly -> "per year";
        };
    }
}
