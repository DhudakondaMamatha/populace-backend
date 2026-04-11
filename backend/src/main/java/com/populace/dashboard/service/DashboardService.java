package com.populace.dashboard.service;

import com.populace.dashboard.dto.DashboardSummaryDto;
import com.populace.domain.Shift;
import com.populace.repository.RoleRepository;
import com.populace.repository.ShiftRepository;
import com.populace.repository.SiteRepository;
import com.populace.repository.StaffMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Service for aggregating dashboard summary data.
 */
@Service
public class DashboardService {

    private final SiteRepository siteRepository;
    private final StaffMemberRepository staffRepository;
    private final RoleRepository roleRepository;
    private final ShiftRepository shiftRepository;

    public DashboardService(
            SiteRepository siteRepository,
            StaffMemberRepository staffRepository,
            RoleRepository roleRepository,
            ShiftRepository shiftRepository) {
        this.siteRepository = siteRepository;
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.shiftRepository = shiftRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(Long businessId) {
        long sites = countSites(businessId);
        long staff = countStaff(businessId);
        long roles = countRoles(businessId);
        long shiftsToday = countTodayShifts(businessId);
        long unallocated = countUnallocatedShifts(businessId);

        return new DashboardSummaryDto(sites, staff, roles, shiftsToday, unallocated);
    }

    private long countSites(Long businessId) {
        return siteRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    private long countStaff(Long businessId) {
        return staffRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    private long countRoles(Long businessId) {
        return roleRepository.countByBusiness_IdAndDeletedAtIsNull(businessId);
    }

    private long countTodayShifts(Long businessId) {
        LocalDate today = LocalDate.now();
        List<Shift> todayShifts = shiftRepository.findByBusiness_IdAndShiftDateBetween(
                businessId, today, today);
        return todayShifts.size();
    }

    private long countUnallocatedShifts(Long businessId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        List<Shift> unfilledShifts = shiftRepository.findUnfilledShifts(
                businessId, monthStart, monthEnd);
        return unfilledShifts.size();
    }
}
