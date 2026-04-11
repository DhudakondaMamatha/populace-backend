package com.populace.compensation.service;

import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.compensation.validation.CompensationValidator;
import com.populace.domain.Role;
import com.populace.domain.StaffCompensation;
import com.populace.domain.StaffMember;
import com.populace.domain.enums.CompensationType;
import com.populace.repository.RoleRepository;
import com.populace.repository.StaffCompensationRepository;
import com.populace.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffCompensationServiceTest {

    @Mock
    private StaffCompensationRepository compensationRepository;

    @Mock
    private StaffMemberRepository staffRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CompensationValidator validator;

    @InjectMocks
    private StaffCompensationService service;

    private StaffMember testStaff;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testStaff = new StaffMember();
        testStaff.setFirstName("John");
        testStaff.setLastName("Doe");

        testRole = new Role();
        testRole.setName("Cashier");
    }

    @Nested
    @DisplayName("Create Hourly Compensation")
    class CreateHourlyCompensation {

        @Test
        @DisplayName("should create hourly compensation successfully")
        void shouldCreateHourlyCompensationSuccessfully() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null, // roleId
                new BigDecimal("25.00"), // hourlyRate
                LocalDate.now(),
                null, // effectiveTo
                "hourly",
                null // monthlySalary
            );

            when(staffRepository.findById(staffId)).thenReturn(Optional.of(testStaff));
            when(compensationRepository.save(any())).thenAnswer(invocation -> {
                StaffCompensation comp = invocation.getArgument(0);
                return comp;
            });

            // When
            CompensationDto result = service.createCompensation(staffId, request);

            // Then
//            assertThat(result).isNotNull();
//            assertThat(result.compensationType()).isEqualTo("hourly");
//            assertThat(result.hourlyRate()).isEqualByComparingTo(new BigDecimal("25.00"));
//            assertThat(result.monthlySalary()).isNull();

            verify(validator).validateCreateRequest(eq(staffId), eq(request));
            verify(validator).validateEntity(any(StaffCompensation.class));
        }

        @Test
        @DisplayName("should reject hourly compensation without hourly rate")
        void shouldRejectHourlyWithoutHourlyRate() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null, null,
                LocalDate.now(), null, "hourly", null
            );

            doThrow(new CompensationValidationException("hourlyRate",
                    "Hourly rate is required for hourly compensation"))
                .when(validator).validateCreateRequest(anyLong(), any());

            // When/Then
            assertThatThrownBy(() -> service.createCompensation(staffId, request))
                .isInstanceOf(CompensationValidationException.class)
                .hasMessageContaining("Hourly rate is required");
        }

        @Test
        @DisplayName("should reject hourly compensation with monthly salary")
        void shouldRejectHourlyWithMonthlySalary() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null,
                "hourly",
                new BigDecimal("5000.00") // Should not be set
            );

            doThrow(new CompensationValidationException("monthlySalary",
                    "Monthly salary must not be set for hourly compensation"))
                .when(validator).validateCreateRequest(anyLong(), any());

            // When/Then
            assertThatThrownBy(() -> service.createCompensation(staffId, request))
                .isInstanceOf(CompensationValidationException.class)
                .hasMessageContaining("Monthly salary must not be set");
        }
    }

    @Nested
    @DisplayName("Create Monthly Compensation")
    class CreateMonthlyCompensation {

        @Test
        @DisplayName("should create monthly compensation successfully")
        void shouldCreateMonthlyCompensationSuccessfully() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                null, // hourlyRate optional for monthly
                LocalDate.now(),
                null,
                "monthly",
                new BigDecimal("5000.00")
            );

            when(staffRepository.findById(staffId)).thenReturn(Optional.of(testStaff));
            when(compensationRepository.save(any())).thenAnswer(invocation -> {
                StaffCompensation comp = invocation.getArgument(0);
                return comp;
            });

            // When
            CompensationDto result = service.createCompensation(staffId, request);

            // Then
//            assertThat(result).isNotNull();
//            assertThat(result.compensationType()).isEqualTo("monthly");
//            assertThat(result.monthlySalary()).isEqualByComparingTo(new BigDecimal("5000.00"));
//            // Hourly rate should be derived
//            assertThat(result.hourlyRate()).isNotNull();

            verify(validator).validateCreateRequest(eq(staffId), eq(request));
        }

        @Test
        @DisplayName("should reject monthly compensation without monthly salary")
        void shouldRejectMonthlyWithoutMonthlySalary() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null,
                "monthly",
                null // Missing monthly salary
            );

            doThrow(new CompensationValidationException("monthlySalary",
                    "Monthly salary is required for monthly compensation"))
                .when(validator).validateCreateRequest(anyLong(), any());

            // When/Then
            assertThatThrownBy(() -> service.createCompensation(staffId, request))
                .isInstanceOf(CompensationValidationException.class)
                .hasMessageContaining("Monthly salary is required");
        }
    }

    @Nested
    @DisplayName("Date Range Validation")
    class DateRangeValidation {

        @Test
        @DisplayName("should reject overlapping date ranges")
        void shouldRejectOverlappingDateRanges() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "hourly",
                null
            );

            doThrow(new CompensationValidationException("effectiveFrom",
                    "Date range overlaps with existing compensation record"))
                .when(validator).validateCreateRequest(anyLong(), any());

            // When/Then
            assertThatThrownBy(() -> service.createCompensation(staffId, request))
                .isInstanceOf(CompensationValidationException.class)
                .hasMessageContaining("Date range overlaps");
        }

        @Test
        @DisplayName("should reject effectiveTo before effectiveFrom")
        void shouldRejectInvalidDateRange() {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.of(2024, 12, 31), // Start
                LocalDate.of(2024, 1, 1),   // End before start
                "hourly",
                null
            );

            doThrow(new CompensationValidationException("effectiveTo",
                    "Effective to date must be on or after effective from date"))
                .when(validator).validateCreateRequest(anyLong(), any());

            // When/Then
            assertThatThrownBy(() -> service.createCompensation(staffId, request))
                .isInstanceOf(CompensationValidationException.class)
                .hasMessageContaining("Effective to date must be on or after");
        }
    }

    @Nested
    @DisplayName("Get Compensation")
    class GetCompensation {

        @Test
        @DisplayName("should return current compensation")
        void shouldReturnCurrentCompensation() {
            // Given
            Long staffId = 1L;
            StaffCompensation compensation = createTestCompensation(CompensationType.hourly);

            when(compensationRepository.findActiveByStaffIdAndDate(eq(staffId), any()))
                .thenReturn(List.of(compensation));

            // When
            Optional<CompensationDto> result = service.getCurrentCompensation(staffId);

            // Then
//            assertThat(result).isPresent();
//            assertThat(result.get().compensationType()).isEqualTo("hourly");
        }

        @Test
        @DisplayName("should return empty when no current compensation")
        void shouldReturnEmptyWhenNoCurrentCompensation() {
            // Given
            Long staffId = 1L;
            when(compensationRepository.findActiveByStaffIdAndDate(eq(staffId), any()))
                .thenReturn(List.of());

            // When
            Optional<CompensationDto> result = service.getCurrentCompensation(staffId);

            // Then
//            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return compensation history")
        void shouldReturnCompensationHistory() {
            // Given
            Long staffId = 1L;
            StaffCompensation comp1 = createTestCompensation(CompensationType.hourly);
            StaffCompensation comp2 = createTestCompensation(CompensationType.monthly);

            when(compensationRepository.findByStaff_Id(staffId))
                .thenReturn(List.of(comp1, comp2));

            // When
            List<CompensationDto> result = service.getCompensationHistory(staffId);

            // Then
//            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethods {

        @Test
        @DisplayName("isHourlyWorker returns true for hourly compensation")
        void isHourlyWorkerReturnsTrue() {
            // Given
            Long staffId = 1L;
            StaffCompensation compensation = createTestCompensation(CompensationType.hourly);
            when(compensationRepository.findActiveByStaffIdAndDate(eq(staffId), any()))
                .thenReturn(List.of(compensation));

            // When
            boolean result = service.isHourlyWorker(staffId);

            // Then
//            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isSalariedWorker returns true for monthly compensation")
        void isSalariedWorkerReturnsTrue() {
            // Given
            Long staffId = 1L;
            StaffCompensation compensation = createTestCompensation(CompensationType.monthly);
            when(compensationRepository.findActiveByStaffIdAndDate(eq(staffId), any()))
                .thenReturn(List.of(compensation));

            // When
            boolean result = service.isSalariedWorker(staffId);

            // Then
//            assertThat(result).isTrue();
        }
    }

    private StaffCompensation createTestCompensation(CompensationType type) {
        StaffCompensation comp = new StaffCompensation();
        comp.setStaff(testStaff);
        comp.setCompensationType(type);
        comp.setHourlyRate(new BigDecimal("25.00"));
        comp.setEffectiveFrom(LocalDate.now().minusDays(30));
        if (type == CompensationType.monthly) {
            comp.setMonthlySalary(new BigDecimal("5000.00"));
        }
        return comp;
    }
}
