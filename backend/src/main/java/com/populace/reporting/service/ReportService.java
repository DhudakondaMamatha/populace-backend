package com.populace.reporting.service;

import com.populace.domain.TimeBlock;
import com.populace.reporting.dto.StaffHoursReportDto;
import com.populace.reporting.dto.StaffHoursReportDto.StaffHoursEntry;
import com.populace.repository.TimeBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating business reports.
 * Uses TimeBlock data for real-time hours tracking.
 */
@Service
public class ReportService {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final TimeBlockRepository timeBlockRepository;

    public ReportService(TimeBlockRepository timeBlockRepository) {
        this.timeBlockRepository = timeBlockRepository;
    }

    @Transactional(readOnly = true)
    public StaffHoursReportDto getStaffHoursSummary(Long businessId) {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        List<TimeBlock> workBlocks = timeBlockRepository.findWorkBlocksByBusinessIdAndDateRange(
                businessId, startDate, endDate);

        if (workBlocks.isEmpty()) {
            return StaffHoursReportDto.empty();
        }

        Map<Long, StaffHoursAggregator> aggregatedByStaff = aggregateHoursByStaff(workBlocks);
        List<StaffHoursEntry> entries = buildStaffHoursEntries(aggregatedByStaff);

        BigDecimal totalWorked = sumWorkedHours(entries);
        BigDecimal totalOvertime = sumOvertimeHours(entries);
        BigDecimal average = calculateAverageHours(totalWorked, totalOvertime, entries.size());

        return new StaffHoursReportDto(
                entries.size(), totalWorked, totalOvertime, average, entries);
    }

    private Map<Long, StaffHoursAggregator> aggregateHoursByStaff(List<TimeBlock> workBlocks) {
        Map<Long, StaffHoursAggregator> byStaff = new HashMap<>();

        for (TimeBlock block : workBlocks) {
            Long staffId = block.getStaff().getId();
            StaffHoursAggregator agg = byStaff.computeIfAbsent(staffId, k -> {
                String name = block.getStaff().getFirstName() + " " + block.getStaff().getLastName();
                String code = block.getStaff().getEmployeeCode();
                return new StaffHoursAggregator(staffId, name, code);
            });

            int durationMinutes = block.getDurationMinutes() != null ? block.getDurationMinutes() : 0;
            BigDecimal hours = BigDecimal.valueOf(durationMinutes).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
            agg.addHours(hours);
        }

        return byStaff;
    }

    private List<StaffHoursEntry> buildStaffHoursEntries(Map<Long, StaffHoursAggregator> aggregated) {
        List<StaffHoursEntry> entries = new ArrayList<>();
        for (StaffHoursAggregator agg : aggregated.values()) {
            entries.add(new StaffHoursEntry(
                    agg.staffId,
                    agg.staffName,
                    agg.employeeCode,
                    agg.workedHours,
                    agg.overtimeHours,
                    agg.workedHours.add(agg.overtimeHours)));
        }
        return entries;
    }

    private BigDecimal sumWorkedHours(List<StaffHoursEntry> entries) {
        return entries.stream()
                .map(StaffHoursEntry::workedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumOvertimeHours(List<StaffHoursEntry> entries) {
        return entries.stream()
                .map(StaffHoursEntry::overtimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageHours(BigDecimal worked, BigDecimal overtime, int count) {
        if (count == 0) return BigDecimal.ZERO;
        BigDecimal total = worked.add(overtime);
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static class StaffHoursAggregator {
        final Long staffId;
        final String staffName;
        final String employeeCode;
        BigDecimal workedHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;

        StaffHoursAggregator(Long staffId, String staffName, String employeeCode) {
            this.staffId = staffId;
            this.staffName = staffName;
            this.employeeCode = employeeCode;
        }

        void addHours(BigDecimal hours) {
            if (hours != null) {
                this.workedHours = this.workedHours.add(hours);
            }
        }
    }
}
