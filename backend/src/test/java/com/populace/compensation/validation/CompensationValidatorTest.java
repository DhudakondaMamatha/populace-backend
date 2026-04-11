package com.populace.compensation.validation;

import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationUpdateRequest;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.domain.StaffCompensation;
import com.populace.domain.StaffMember;
import com.populace.domain.enums.CompensationType;
import com.populace.repository.StaffCompensationRepository;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationValidatorTest {

    @Mock
    private StaffCompensationRepository compensationRepository;

    @InjectMocks
    private CompensationValidator validator;

    private StaffMember testStaff;

    @BeforeEach
    void setUp() {
        testStaff = new StaffMember();
        testStaff.setFirstName("John");
        testStaff.setLastName("Doe");
    }

    @Nested
    @DisplayName("Hourly Compensation Validation")
    class HourlyValidation {

        @Test
        @DisplayName("should accept valid hourly compensation")
        void shouldAcceptValidHourlyCompensation() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(),
                null,
                "hourly",
                null
            );

            when(compensationRepository.findByStaff_Id(anyLong())).thenReturn(List.of());

            // When/Then
            assertThatCode(() -> validator.validateCreateRequest(1L, request))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject hourly without hourlyRate")
        void shouldRejectHourlyWithoutHourlyRate() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null, null,
                LocalDate.now(), null, "hourly", null
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("hourlyRate"));
        }

        @Test
        @DisplayName("should reject hourly with zero hourlyRate")
        void shouldRejectHourlyWithZeroRate() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                BigDecimal.ZERO,
                LocalDate.now(), null,
                "hourly",
                null
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("greater than zero"));
        }

        @Test
        @DisplayName("should reject hourly with monthlySalary set")
        void shouldRejectHourlyWithMonthlySalary() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null,
                "hourly",
                new BigDecimal("5000.00") // Should not be set
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("monthlySalary"));
        }
    }

    @Nested
    @DisplayName("Monthly Compensation Validation")
    class MonthlyValidation {

        @Test
        @DisplayName("should accept valid monthly compensation")
        void shouldAcceptValidMonthlyCompensation() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("28.85"), // Optional for monthly
                LocalDate.now(),
                null,
                "monthly",
                new BigDecimal("5000.00")
            );

            when(compensationRepository.findByStaff_Id(anyLong())).thenReturn(List.of());

            // When/Then
            assertThatCode(() -> validator.validateCreateRequest(1L, request))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject monthly without monthlySalary")
        void shouldRejectMonthlyWithoutSalary() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null,
                "monthly",
                null // Missing
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("monthlySalary"));
        }

        @Test
        @DisplayName("should reject monthly with zero salary")
        void shouldRejectMonthlyWithZeroSalary() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null,
                "monthly",
                BigDecimal.ZERO
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("greater than zero"));
        }
    }

    @Nested
    @DisplayName("Date Range Validation")
    class DateRangeValidation {

        @Test
        @DisplayName("should reject missing effectiveFrom")
        void shouldRejectMissingEffectiveFrom() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                null, // Missing
                null,
                "hourly",
                null
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("effectiveFrom"));
        }

        @Test
        @DisplayName("should reject effectiveTo before effectiveFrom")
        void shouldRejectInvalidDateRange() {
            // Given
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.of(2024, 12, 31),
                LocalDate.of(2024, 1, 1), // Before start
                "hourly",
                null
            );

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("effectiveTo"));
        }

        @Test
        @DisplayName("should reject overlapping date ranges")
        void shouldRejectOverlappingRanges() {
            // Given
            StaffCompensation existing = new StaffCompensation();
            existing.setStaff(testStaff);
            existing.setEffectiveFrom(LocalDate.of(2024, 1, 1));
            existing.setEffectiveTo(LocalDate.of(2024, 12, 31));
            existing.setCompensationType(CompensationType.hourly);
            existing.setHourlyRate(new BigDecimal("20.00"));

            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.of(2024, 6, 1), // Overlaps
                LocalDate.of(2025, 6, 1),
                "hourly",
                null
            );

            when(compensationRepository.findByStaff_Id(anyLong())).thenReturn(List.of(existing));

            // When/Then
            assertThatThrownBy(() -> validator.validateCreateRequest(1L, request))
                .isInstanceOf(CompensationValidationException.class)
                .extracting(ex -> ((CompensationValidationException) ex).getErrors())
                .asList()
                .anyMatch(e -> e.toString().contains("overlaps"));
        }
    }

    @Nested
    @DisplayName("Update Validation")
    class UpdateValidation {

        @Test
        @DisplayName("should validate update request against existing record")
        void shouldValidateUpdateRequest() {
            // Given
            StaffCompensation existing = new StaffCompensation();
            existing.setStaff(testStaff);
            existing.setCompensationType(CompensationType.hourly);
            existing.setHourlyRate(new BigDecimal("25.00"));
            existing.setEffectiveFrom(LocalDate.now().minusDays(30));

            CompensationUpdateRequest request = new CompensationUpdateRequest(
                new BigDecimal("30.00"), // New rate
                null, null, null, null
            );

            // When/Then
            assertThatCode(() -> validator.validateUpdateRequest(existing, request))
                .doesNotThrowAnyException();
        }
    }
}
