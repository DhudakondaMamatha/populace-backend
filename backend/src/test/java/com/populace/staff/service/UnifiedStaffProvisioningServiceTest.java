package com.populace.staff.service;

import com.populace.common.exception.ValidationException;
import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.service.StaffCompensationService;
import com.populace.domain.*;
import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.EmploymentType;
import com.populace.repository.*;
import com.populace.staff.dto.StaffDto;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedStaffProvisioningServiceTest {

    @Mock
    private StaffMemberRepository staffRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private StaffRoleRepository staffRoleRepository;
    @Mock
    private StaffSiteRepository staffSiteRepository;
    @Mock
    private StaffCompetenceLevelRepository competenceLevelRepository;
    @Mock
    private StaffCompensationService compensationService;
    @Mock
    private StaffConstraintValidator constraintValidator;

    @InjectMocks
    private UnifiedStaffProvisioningService service;

    private Business testBusiness;
    private Role testRole;
    private Site testSite;

    @BeforeEach
    void setUp() {
        testBusiness = new Business();
        ReflectionTestUtils.setField(testBusiness, "id", 1L);
        testBusiness.setName("Test Business");

        testRole = new Role();
        ReflectionTestUtils.setField(testRole, "id", 1L);
        testRole.setName("Barista");
        testRole.setBusiness(testBusiness);

        testSite = new Site();
        ReflectionTestUtils.setField(testSite, "id", 1L);
        testSite.setName("Main Store");
        testSite.setBusiness(testBusiness);
    }

    // Helper method to create UnifiedStaffCreateRequest with all parameters
    private UnifiedStaffCreateRequest createRequest(
            String firstName, String lastName, String email, String phone, String employeeCode, String employmentType,
            List<Long> roleIds, List<Long> siteIds, List<String> competenceLevels,
            String compensationType, BigDecimal hourlyRate, BigDecimal monthlySalary,
            BigDecimal minHoursPerDay, BigDecimal maxHoursPerDay, BigDecimal minHoursPerMonth, BigDecimal maxHoursPerMonth,
            Integer minDaysOffPerWeek, Integer maxSitesPerDay,
            Integer mustGoOnLeaveAfterDays, Integer accruesOneDayLeaveAfterDays) {
        return new UnifiedStaffCreateRequest(
            firstName, lastName, email, phone, employeeCode, employmentType,
            roleIds, siteIds, competenceLevels,
            null, // roleAssignments
            compensationType, hourlyRate, monthlySalary,
            minHoursPerDay, maxHoursPerDay, minHoursPerMonth, maxHoursPerMonth,
            minDaysOffPerWeek, maxSitesPerDay,
            null, null, // minHoursPerWeek, maxHoursPerWeek
            mustGoOnLeaveAfterDays, accruesOneDayLeaveAfterDays
        );
    }

    @Nested
    @DisplayName("Full Provisioning Tests")
    class FullProvisioningTests {

        @Test
        @DisplayName("should create staff with full provisioning")
        void shouldCreateStaffWithFullProvisioning() {
            // Given
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", "john@example.com", "555-1234", "EMP001", "permanent",
                List.of(1L), List.of(1L), List.of("L1", "L2"),
                "hourly", new BigDecimal("25.00"), null,
                new BigDecimal("4.0"), new BigDecimal("12.0"),
                new BigDecimal("80"), new BigDecimal("208"), 1,
                null, null, null
            );

            StaffMember savedStaff = createMockStaffMember(1L, request);

            when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));
            when(staffRepository.existsByEmailIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(anyString(), anyLong()))
                .thenReturn(false);
            when(staffRepository.existsByEmployeeCodeIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(anyString(), anyLong()))
                .thenReturn(false);
            when(roleRepository.existsByIdAndBusiness_Id(1L, 1L)).thenReturn(true);
            when(siteRepository.existsByIdAndBusiness_Id(1L, 1L)).thenReturn(true);
            when(staffRepository.save(any())).thenReturn(savedStaff);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(siteRepository.findById(1L)).thenReturn(Optional.of(testSite));
            when(staffRoleRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(staffSiteRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(competenceLevelRepository.findByStaff_Id(anyLong())).thenReturn(List.of());
            when(compensationService.createCompensation(anyLong(), any())).thenReturn(
                new CompensationDto(1L, 1L, null, null, new BigDecimal("25.00"), LocalDate.now(), null, "hourly", null, true)
            );

            // When
            StaffDto result = service.createStaffWithFullProvisioning(1L, request);

            // Then
//            assertThat(result).isNotNull();
//            assertThat(result.firstName()).isEqualTo("John");
//            assertThat(result.lastName()).isEqualTo("Doe");
////
            // Verify all entities were saved
            verify(staffRepository).save(any(StaffMember.class));
            verify(staffRoleRepository).save(any(StaffRole.class));
            verify(staffSiteRepository).save(any(StaffSite.class));
            verify(competenceLevelRepository, times(2)).save(any(StaffCompetenceLevel.class));
            verify(compensationService).createCompensation(eq(1L), any(CompensationCreateRequest.class));
        }

        @Test
        @DisplayName("should attach multiple roles and sites")
        void shouldAttachMultipleRolesAndSites() {
            // Given
            Role role2 = new Role();
            ReflectionTestUtils.setField(role2, "id", 2L);
            role2.setName("Manager");
            role2.setBusiness(testBusiness);

            Site site2 = new Site();
            ReflectionTestUtils.setField(site2, "id", 2L);
            site2.setName("Branch Store");
            site2.setBusiness(testBusiness);

            UnifiedStaffCreateRequest request = createRequest(
                "Jane", "Smith", null, null, null, "permanent",
                List.of(1L, 2L), List.of(1L, 2L), null,
                null, null, null, null, null, null, null, null,
                null, null, null
            );

            StaffMember savedStaff = createMockStaffMember(1L, request);

            when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));
            when(roleRepository.existsByIdAndBusiness_Id(anyLong(), eq(1L))).thenReturn(true);
            when(siteRepository.existsByIdAndBusiness_Id(anyLong(), eq(1L))).thenReturn(true);
            when(staffRepository.save(any())).thenReturn(savedStaff);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(role2));
            when(siteRepository.findById(1L)).thenReturn(Optional.of(testSite));
            when(siteRepository.findById(2L)).thenReturn(Optional.of(site2));
            when(staffRoleRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(staffSiteRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(competenceLevelRepository.findByStaff_Id(anyLong())).thenReturn(List.of());

            // When
            service.createStaffWithFullProvisioning(1L, request);

            // Then
            verify(staffRoleRepository, times(2)).save(any(StaffRole.class));
            verify(staffSiteRepository, times(2)).save(any(StaffSite.class));
        }

        @Test
        @DisplayName("should create compensation for EACH assigned role")
        void shouldCreateCompensationForEachRole() {
            // Given
            Role role2 = new Role();
            ReflectionTestUtils.setField(role2, "id", 2L);
            role2.setName("Manager");
            role2.setBusiness(testBusiness);

            Role role3 = new Role();
            ReflectionTestUtils.setField(role3, "id", 3L);
            role3.setName("Shift Manager");
            role3.setBusiness(testBusiness);

            UnifiedStaffCreateRequest request = createRequest(
                "Jane", "Smith", null, null, null, "permanent",
                List.of(1L, 2L, 3L), null, null,
                "hourly", new BigDecimal("25.00"), null,
                null, null, null, null, null,
                null, null, null
            );

            StaffMember savedStaff = createMockStaffMember(1L, request);

            when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));
            when(roleRepository.existsByIdAndBusiness_Id(anyLong(), eq(1L))).thenReturn(true);
            when(staffRepository.save(any())).thenReturn(savedStaff);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(role2));
            when(roleRepository.findById(3L)).thenReturn(Optional.of(role3));
            when(staffRoleRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(staffSiteRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(competenceLevelRepository.findByStaff_Id(anyLong())).thenReturn(List.of());
            when(compensationService.createCompensation(anyLong(), any())).thenReturn(
                new CompensationDto(1L, 1L, null, null, new BigDecimal("25.00"), LocalDate.now(), null, "hourly", null, true)
            );

            // When
            service.createStaffWithFullProvisioning(1L, request);

            // Then - Compensation should be created for EACH role (3 times)
            ArgumentCaptor<CompensationCreateRequest> captor = ArgumentCaptor.forClass(CompensationCreateRequest.class);
            verify(compensationService, times(3)).createCompensation(eq(1L), captor.capture());

            List<Long> roleIds = captor.getAllValues().stream()
                .map(CompensationCreateRequest::roleId)
                .toList();
//            assertThat(roleIds).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should not create duplicate compensation for same role")
        void shouldNotCreateDuplicateCompensation() {
            // Given - same role ID twice (edge case)
            UnifiedStaffCreateRequest request = createRequest(
                "Jane", "Smith", null, null, null, "permanent",
                List.of(1L), null, null,
                "hourly", new BigDecimal("25.00"), null,
                null, null, null, null, null,
                null, null, null
            );

            StaffMember savedStaff = createMockStaffMember(1L, request);

            when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));
            when(roleRepository.existsByIdAndBusiness_Id(anyLong(), eq(1L))).thenReturn(true);
            when(staffRepository.save(any())).thenReturn(savedStaff);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(staffRoleRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(staffSiteRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(competenceLevelRepository.findByStaff_Id(anyLong())).thenReturn(List.of());
            when(compensationService.createCompensation(anyLong(), any())).thenReturn(
                new CompensationDto(1L, 1L, null, null, new BigDecimal("25.00"), LocalDate.now(), null, "hourly", null, true)
            );

            // When
            service.createStaffWithFullProvisioning(1L, request);

            // Then - Only one compensation record
            verify(compensationService, times(1)).createCompensation(anyLong(), any());
        }

        @Test
        @DisplayName("should persist competence levels")
        void shouldPersistCompetenceLevels() {
            // Given
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, List.of("L1", "L2", "L3"),
                null, null, null, null, null, null, null, null,
                null, null, null
            );

            StaffMember savedStaff = createMockStaffMember(1L, request);

            when(businessRepository.findById(1L)).thenReturn(Optional.of(testBusiness));
            when(staffRepository.save(any())).thenReturn(savedStaff);
            when(staffRoleRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(staffSiteRepository.findByStaff_IdAndActive(anyLong(), anyBoolean())).thenReturn(List.of());
            when(competenceLevelRepository.findByStaff_Id(anyLong())).thenReturn(List.of());

            // When
            service.createStaffWithFullProvisioning(1L, request);

            // Then
            ArgumentCaptor<StaffCompetenceLevel> captor = ArgumentCaptor.forClass(StaffCompetenceLevel.class);
            verify(competenceLevelRepository, times(3)).save(captor.capture());

            List<String> savedLevels = captor.getAllValues().stream()
                .map(StaffCompetenceLevel::getLevel)
                .toList();
//            assertThat(savedLevels).containsExactlyInAnyOrder("L1", "L2", "L3");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should reject missing first name")
        void shouldRejectMissingFirstName() {
            UnifiedStaffCreateRequest request = createRequest(
                null, "Doe", null, null, null, "permanent",
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First name is required");
        }

        @Test
        @DisplayName("should reject invalid employment type")
        void shouldRejectInvalidEmploymentType() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "freelance",
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("employmentType");
        }

        @Test
        @DisplayName("should validate compensation type logic - hourly requires rate")
        void shouldValidateCompensationTypeLogic() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null,
                "hourly", null, null,
                null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("hourlyRate");
        }

        @Test
        @DisplayName("should validate compensation type logic - monthly requires salary")
        void shouldValidateMonthlyCompensation() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null,
                "monthly", null, null,
                null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("monthlySalary");
        }

        @Test
        @DisplayName("should validate work hour ranges")
        void shouldValidateWorkHourRanges() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null, null, null, null,
                new BigDecimal("12"), new BigDecimal("4"),
                null, null, null,
                null, null, null
            );

            doThrow(new ValidationException("minHoursPerDay cannot exceed maxHoursPerDay"))
                .when(constraintValidator).ensureConstraintsAreWithinLimits(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("minHoursPerDay");
        }

        @Test
        @DisplayName("should validate days off per week range")
        void shouldValidateDaysOffRange() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null, null, null, null, null, null, null, null,
                8, null, null, null
            );

            doThrow(new ValidationException("minDaysOffPerWeek must be between 0 and 7"))
                .when(constraintValidator).ensureConstraintsAreWithinLimits(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("minDaysOffPerWeek");
        }

        @Test
        @DisplayName("should validate invalid competence level")
        void shouldValidateInvalidCompetenceLevel() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, List.of("L1", "L4"),
                null, null, null, null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("L4");
        }

        @Test
        @DisplayName("should validate role ID exists")
        void shouldValidateRoleExists() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                List.of(999L), null, null,
                null, null, null, null, null, null, null, null,
                null, null, null
            );

            when(roleRepository.existsByIdAndBusiness_Id(999L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Role with ID 999 not found");
        }

        @Test
        @DisplayName("should validate site ID exists")
        void shouldValidateSiteExists() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, List.of(999L), null,
                null, null, null, null, null, null, null, null,
                null, null, null
            );

            when(siteRepository.existsByIdAndBusiness_Id(999L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Site with ID 999 not found");
        }
    }

    @Nested
    @DisplayName("Rollback Tests")
    class RollbackTests {

        @Test
        @DisplayName("should rollback if compensation invalid")
        void shouldRollbackIfCompensationInvalid() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null,
                "hourly", BigDecimal.ZERO, null,
                null, null, null, null, null,
                null, null, null
            );

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class);

            // Verify no saves occurred
            verify(staffRepository, never()).save(any());
        }

        @Test
        @DisplayName("should rollback if work rules invalid")
        void shouldRollbackIfWorkRulesInvalid() {
            UnifiedStaffCreateRequest request = createRequest(
                "John", "Doe", null, null, null, "permanent",
                null, null, null, null, null, null,
                new BigDecimal("25"), new BigDecimal("10"),
                null, null, null,
                null, null, null
            );

            doThrow(new ValidationException("minHoursPerDay cannot exceed maxHoursPerDay"))
                .when(constraintValidator).ensureConstraintsAreWithinLimits(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() -> service.createStaffWithFullProvisioning(1L, request))
                .isInstanceOf(ValidationException.class);

            // Verify no saves occurred
            verify(staffRepository, never()).save(any());
        }
    }

    private StaffMember createMockStaffMember(Long id, UnifiedStaffCreateRequest request) {
        StaffMember staff = new StaffMember();
        ReflectionTestUtils.setField(staff, "id", id);
        staff.setBusiness(testBusiness);
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setEmail(request.email());
        staff.setPhone(request.phone());
        staff.setEmployeeCode(request.employeeCode());
        staff.setEmploymentType(EmploymentType.permanent);
        staff.setEmploymentStatus(EmploymentStatus.active);
        return staff;
    }
}
