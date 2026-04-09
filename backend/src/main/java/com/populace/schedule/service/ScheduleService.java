package com.populace.schedule.service;

import com.populace.domain.*;
import com.populace.domain.enums.BlockType;
import com.populace.repository.*;
import com.populace.schedule.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Schedule service using TimeBlock-based allocation.
 * Reads from time_blocks table for allocation data.
 */
@Service
public class ScheduleService {

    private final StaffMemberRepository staffRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final StaffSiteRepository staffSiteRepository;

    public ScheduleService(
            StaffMemberRepository staffRepository,
            TimeBlockRepository timeBlockRepository,
            StaffRoleRepository staffRoleRepository,
            StaffSiteRepository staffSiteRepository) {
        this.staffRepository = staffRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.staffSiteRepository = staffSiteRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffScheduleDto> getScheduleData(
            Long businessId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleFilterParams filters) {

        List<StaffMember> activeStaff = staffRepository.findActiveStaff(businessId);

        return activeStaff.stream()
            .filter(staff -> matchesFilters(staff, filters))
            .map(staff -> buildStaffSchedule(staff, startDate, endDate, filters))
            .filter(schedule -> !filters.hasAnyFilter() || hasShiftsOrMatchesResourceName(schedule, filters))
            .toList();
    }

    private boolean matchesFilters(StaffMember staff, ScheduleFilterParams filters) {
        if (filters.hasResourceNameFilter()) {
            String fullName = staff.getFullName().toLowerCase();
            if (!fullName.contains(filters.getResourceNameLower())) {
                return false;
            }
        }

        if (filters.hasSiteFilter()) {
            boolean hasMatchingSite = staffSiteRepository
                .findByStaffIdAndActiveWithSite(staff.getId(), true)
                .stream()
                .anyMatch(ss -> ss.getSite().getId().equals(filters.siteId()));
            if (!hasMatchingSite) {
                return false;
            }
        }

        if (filters.hasRoleFilter()) {
            boolean hasMatchingRole = staffRoleRepository
                .findByStaffIdAndActiveWithRole(staff.getId(), true)
                .stream()
                .anyMatch(sr -> sr.getRole().getId().equals(filters.roleId()));
            if (!hasMatchingRole) {
                return false;
            }
        }

        return true;
    }

    private boolean hasShiftsOrMatchesResourceName(StaffScheduleDto schedule, ScheduleFilterParams filters) {
        if (filters.hasResourceNameFilter()) {
            return true;
        }
        return !schedule.shifts().isEmpty();
    }

    @Transactional(readOnly = true)
    public WeeklySummaryDto getStaffWeeklySummary(
            Long businessId,
            Long staffId,
            LocalDate startDate,
            LocalDate endDate) {

        List<TimeBlock> blocks = timeBlockRepository
            .findWorkBlocksByStaffIdAndDateRange(staffId, startDate, endDate);

        return calculateWeeklySummaryFromBlocks(blocks);
    }

    private StaffScheduleDto buildStaffSchedule(
            StaffMember staff,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleFilterParams filters) {

        List<ScheduleRoleDto> roles = getStaffRoles(staff.getId());
        List<ShiftAllocationDto> shifts = getStaffShifts(staff.getId(), startDate, endDate, filters);
        WeeklySummaryDto summary = calculateWeeklySummaryFromShiftDtos(shifts);

        return new StaffScheduleDto(
            staff.getId(),
            staff.getFullName(),
            roles,
            shifts,
            summary
        );
    }

    private List<ScheduleRoleDto> getStaffRoles(Long staffId) {
        List<StaffRole> staffRoles = staffRoleRepository
            .findByStaffIdAndActiveWithRole(staffId, true);

        return staffRoles.stream()
            .map(this::toScheduleRoleDto)
            .toList();
    }

    private ScheduleRoleDto toScheduleRoleDto(StaffRole staffRole) {
        Role role = staffRole.getRole();
        return new ScheduleRoleDto(
            role.getId(),
            role.getName(),
            role.getColor()
        );
    }

    private List<ShiftAllocationDto> getStaffShifts(
            Long staffId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleFilterParams filters) {

        List<TimeBlock> workBlocks = timeBlockRepository
            .findWorkBlocksByStaffIdAndDateRange(staffId, startDate, endDate);

        Map<Long, BreakInfo> breakInfoByShiftId = computeBreakInfoByShift(staffId, startDate, endDate);

        return workBlocks.stream()
            .filter(block -> matchesBlockFilters(block, filters))
            .map(block -> toShiftAllocationDto(block, breakInfoByShiftId))
            .toList();
    }

    /**
     * For each shift, aggregates total break minutes and captures the first
     * break_period block's start/end time from time_blocks.
     */
    private Map<Long, BreakInfo> computeBreakInfoByShift(Long staffId, LocalDate startDate, LocalDate endDate) {
        List<TimeBlock> allBlocks = timeBlockRepository
            .findActiveBlocksInDateRange(staffId, startDate, endDate);

        Map<Long, BreakInfo> result = new HashMap<>();
        for (TimeBlock block : allBlocks) {
            if (block.isBreakBlock() && block.getShiftId() != null) {
                int minutes = block.getDurationMinutes() != null ? block.getDurationMinutes() : 0;
                result.merge(
                    block.getShiftId(),
                    new BreakInfo(
                        minutes,
                        block.getStartTime() != null
                            ? block.getStartTime().atZone(ZoneOffset.UTC).toLocalTime() : null,
                        block.getEndTime() != null
                            ? block.getEndTime().atZone(ZoneOffset.UTC).toLocalTime() : null
                    ),
                    (existing, next) -> new BreakInfo(
                        existing.minutes() + next.minutes(),
                        existing.startTime(),   // keep first break's start
                        next.endTime()          // extend to last break's end
                    )
                );
            }
        }
        return result;
    }

    private record BreakInfo(int minutes, java.time.LocalTime startTime, java.time.LocalTime endTime) {}

    private boolean matchesBlockFilters(TimeBlock block, ScheduleFilterParams filters) {
        Shift shift = block.getShift();

        if (filters.hasSiteFilter()) {
            if (!shift.getSite().getId().equals(filters.siteId())) {
                return false;
            }
        }

        if (filters.hasRoleFilter()) {
            if (!shift.getRole().getId().equals(filters.roleId())) {
                return false;
            }
        }

        return true;
    }

    private ShiftAllocationDto toShiftAllocationDto(TimeBlock block, Map<Long, BreakInfo> breakInfoByShiftId) {
        Shift shift = block.getShift();
        Role role = shift.getRole();
        var site = shift.getSite();

        BreakInfo breakInfo = breakInfoByShiftId.get(shift.getId());
        int breakMinutes = breakInfo != null ? breakInfo.minutes() : getBreakMinutesSafe(shift);
        java.time.LocalTime breakStartTime = breakInfo != null ? breakInfo.startTime() : null;
        java.time.LocalTime breakEndTime   = breakInfo != null ? breakInfo.endTime()   : null;

        return new ShiftAllocationDto(
            block.getId(),
            shift.getId(),
            shift.getShiftDate(),
            block.getStartTime().atZone(ZoneOffset.UTC).toLocalTime(),
            block.getEndTime().atZone(ZoneOffset.UTC).toLocalTime(),
            BigDecimal.valueOf(block.getDurationMinutes() != null ? block.getDurationMinutes() / 60.0 : 0),
            breakMinutes,
            breakStartTime,
            breakEndTime,
            role.getId(),
            role.getName(),
            site.getId(),
            site.getName()
        );
    }

    private Integer getBreakMinutesSafe(Shift shift) {
        Integer breakMinutes = shift.getBreakDurationMinutes();
        return breakMinutes != null ? breakMinutes : 0;
    }

    private WeeklySummaryDto calculateWeeklySummaryFromBlocks(List<TimeBlock> blocks) {
        int totalMinutes = 0;
        int totalBreakMinutes = 0;

        for (TimeBlock block : blocks) {
            if (block.getBlockType() == BlockType.work) {
                totalMinutes += block.getDurationMinutes() != null ? block.getDurationMinutes() : 0;
            } else if (block.getBlockType() == BlockType.break_period) {
                totalBreakMinutes += block.getDurationMinutes() != null ? block.getDurationMinutes() : 0;
            }
        }

        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes / 60.0);
        return new WeeklySummaryDto(totalHours, totalBreakMinutes);
    }

    private WeeklySummaryDto calculateWeeklySummaryFromShiftDtos(List<ShiftAllocationDto> shifts) {
        BigDecimal totalHours = BigDecimal.ZERO;
        int totalBreakMinutes = 0;

        for (ShiftAllocationDto shift : shifts) {
            totalHours = addHoursSafe(totalHours, shift.totalHours());
            totalBreakMinutes += getBreakMinutesSafe(shift.breakMinutes());
        }

        return new WeeklySummaryDto(totalHours, totalBreakMinutes);
    }

    private BigDecimal addHoursSafe(BigDecimal current, BigDecimal toAdd) {
        if (toAdd == null) {
            return current;
        }
        return current.add(toAdd);
    }

    private int getBreakMinutesSafe(Integer breakMinutes) {
        return breakMinutes != null ? breakMinutes : 0;
    }
}
