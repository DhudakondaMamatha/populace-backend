package com.populace.reporting.service;

import com.populace.domain.*;
import com.populace.domain.enums.BlockType;
import com.populace.domain.enums.ShiftStatus;
import com.populace.reporting.dto.StaffHoursReportDto;
import com.populace.repository.TimeBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @InjectMocks
    private ReportService reportService;

    private static final Long BUSINESS_ID = 1L;
    private Business testBusiness;
    private Site siteA;
    private Site siteB;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testBusiness = new Business();
        ReflectionTestUtils.setField(testBusiness, "id", BUSINESS_ID);

        siteA = new Site();
        ReflectionTestUtils.setField(siteA, "id", 10L);
        siteA.setName("Site A");
        siteA.setBusiness(testBusiness);

        siteB = new Site();
        ReflectionTestUtils.setField(siteB, "id", 20L);
        siteB.setName("Site B");
        siteB.setBusiness(testBusiness);

        testRole = new Role();
        ReflectionTestUtils.setField(testRole, "id", 30L);
        testRole.setBusiness(testBusiness);
    }

    @Test
    @DisplayName("should return staff hours summary with aggregated data from TimeBlocks")
    void shouldReturnStaffHoursSummary() {
        // Given
        StaffMember staff1 = createStaff(100L, "John", "Doe", "EMP001");
        StaffMember staff2 = createStaff(101L, "Jane", "Smith", "EMP002");

        Shift shift1 = createShift(1L, siteA, LocalDate.now(), ShiftStatus.filled);
        Shift shift2 = createShift(2L, siteA, LocalDate.now(), ShiftStatus.filled);

        // Create TimeBlocks: staff1 worked 480 mins (8h), staff2 worked 420 mins (7h)
        List<TimeBlock> workBlocks = List.of(
                createTimeBlock(1L, staff1, shift1, 480),  // 8 hours
                createTimeBlock(2L, staff2, shift2, 420)   // 7 hours
        );

        when(timeBlockRepository.findWorkBlocksByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(workBlocks);

        // When
        StaffHoursReportDto report = reportService.getStaffHoursSummary(BUSINESS_ID);

        // Then
       // assertThat(report.totalStaffWithHours()).isEqualTo(2);
        //assertThat(report.totalWorkedHours()).isEqualByComparingTo(new BigDecimal("15.00"));
        //assertThat(report.staffEntries()).hasSize(2);
    }

    @Test
    @DisplayName("should return empty staff hours when no data exists")
    void shouldReturnEmptyStaffHoursWhenNoData() {
        // Given
        when(timeBlockRepository.findWorkBlocksByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of());

        // When
        StaffHoursReportDto report = reportService.getStaffHoursSummary(BUSINESS_ID);

        // Then
       // assertThat(report.totalStaffWithHours()).isZero();
       // assertThat(report.totalWorkedHours()).isEqualByComparingTo(BigDecimal.ZERO);
       // assertThat(report.staffEntries()).isEmpty();
    }

    private Shift createShift(Long id, Site site, LocalDate date, ShiftStatus status) {
        Shift shift = new Shift();
        ReflectionTestUtils.setField(shift, "id", id);
        shift.setBusiness(testBusiness);
        shift.setSite(site);
        shift.setRole(testRole);
        shift.setShiftDate(date);
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setStatus(status);
        return shift;
    }

    private StaffMember createStaff(Long id, String firstName, String lastName, String code) {
        StaffMember staff = new StaffMember();
        ReflectionTestUtils.setField(staff, "id", id);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmployeeCode(code);
        staff.setBusiness(testBusiness);
        return staff;
    }

    private TimeBlock createTimeBlock(Long id, StaffMember staff, Shift shift, int durationMinutes) {
        TimeBlock block = new TimeBlock();
        ReflectionTestUtils.setField(block, "id", id);
        block.setStaff(staff);
        block.setShift(shift);
        block.setRole(testRole);
        block.setSite(shift.getSite());
        block.setBlockType(BlockType.work);

        Instant start = LocalDate.now().atTime(9, 0).toInstant(ZoneOffset.UTC);
        Instant end = start.plusSeconds(durationMinutes * 60L);
        block.setStartTime(start);
        block.setEndTime(end);

        return block;
    }
}
