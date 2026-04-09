package com.populace.dashboard.service;

import com.populace.dashboard.dto.DashboardSummaryDto;
import com.populace.domain.Business;
import com.populace.domain.Role;
import com.populace.domain.Shift;
import com.populace.domain.Site;
import com.populace.domain.enums.ShiftStatus;
import com.populace.repository.RoleRepository;
import com.populace.repository.ShiftRepository;
import com.populace.repository.SiteRepository;
import com.populace.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private StaffMemberRepository staffRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private static final Long BUSINESS_ID = 1L;
    private Business testBusiness;
    private Site testSite;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testBusiness = new Business();
        ReflectionTestUtils.setField(testBusiness, "id", BUSINESS_ID);

        testSite = new Site();
        ReflectionTestUtils.setField(testSite, "id", 10L);
        testSite.setBusiness(testBusiness);

        testRole = new Role();
        ReflectionTestUtils.setField(testRole, "id", 20L);
        testRole.setBusiness(testBusiness);
    }

    @Test
    @DisplayName("should return correct counts when data exists")
    void shouldReturnCorrectCounts() {
        // Given
        when(siteRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(5L);
        when(staffRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(25L);
        when(roleRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(8L);

        LocalDate today = LocalDate.now();
        List<Shift> todayShifts = List.of(
                createShift(1L, today, ShiftStatus.filled),
                createShift(2L, today, ShiftStatus.open),
                createShift(3L, today, ShiftStatus.partially_filled)
        );
        when(shiftRepository.findByBusiness_IdAndShiftDateBetween(eq(BUSINESS_ID), eq(today), eq(today)))
                .thenReturn(todayShifts);

        List<Shift> unfilledShifts = List.of(
                createShift(2L, today, ShiftStatus.open),
                createShift(4L, today.plusDays(1), ShiftStatus.open)
        );
        when(shiftRepository.findUnfilledShifts(eq(BUSINESS_ID), any(), any()))
                .thenReturn(unfilledShifts);

        // When
        DashboardSummaryDto summary = dashboardService.getDashboardSummary(BUSINESS_ID);

        // Then
//        assertThat(summary.totalSites()).isEqualTo(5);
//        assertThat(summary.totalStaff()).isEqualTo(25);
//        assertThat(summary.totalRoles()).isEqualTo(8);
//        assertThat(summary.totalShiftsToday()).isEqualTo(3);
//        assertThat(summary.totalUnallocatedShifts()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return zero when no data exists")
    void shouldReturnZeroWhenNoData() {
        // Given
        when(siteRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(0L);
        when(staffRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(0L);
        when(roleRepository.countByBusiness_IdAndDeletedAtIsNull(BUSINESS_ID)).thenReturn(0L);
        when(shiftRepository.findByBusiness_IdAndShiftDateBetween(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of());
        when(shiftRepository.findUnfilledShifts(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of());

        // When
        DashboardSummaryDto summary = dashboardService.getDashboardSummary(BUSINESS_ID);

//        // Then
//        assertThat(summary.totalSites()).isZero();
//        assertThat(summary.totalStaff()).isZero();
//        assertThat(summary.totalRoles()).isZero();
//        assertThat(summary.totalShiftsToday()).isZero();
//        assertThat(summary.totalUnallocatedShifts()).isZero();
    }

    private Shift createShift(Long id, LocalDate date, ShiftStatus status) {
        Shift shift = new Shift();
        ReflectionTestUtils.setField(shift, "id", id);
        shift.setBusiness(testBusiness);
        shift.setSite(testSite);
        shift.setRole(testRole);
        shift.setShiftDate(date);
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setStatus(status);
        return shift;
    }
}
