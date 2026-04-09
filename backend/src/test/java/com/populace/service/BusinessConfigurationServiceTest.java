package com.populace.service;

import com.populace.domain.Business;
import com.populace.domain.BusinessConfiguration;
import com.populace.repository.BusinessConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessConfigurationServiceTest {

    @Mock
    private BusinessConfigurationRepository repository;

    @InjectMocks
    private BusinessConfigurationService service;

    private Business testBusiness;

    @BeforeEach
    void setUp() {
        testBusiness = new Business();
        ReflectionTestUtils.setField(testBusiness, "id", 1L);
        testBusiness.setName("Test Business");
    }

    @Nested
    @DisplayName("Get Monthly Tolerance Percent")
    class GetMonthlyTolerancePercent {

        @Test
        @DisplayName("should return configured tolerance when configuration exists")
        void shouldReturnConfiguredToleranceWhenConfigurationExists() {
            // Given
            Long businessId = 1L;
            BigDecimal configuredTolerance = new BigDecimal("15.00");

            BusinessConfiguration config = new BusinessConfiguration();
            ReflectionTestUtils.setField(config, "id", 1L);
            config.setBusiness(testBusiness);
            config.setMonthlyHourTolerancePercent(configuredTolerance);

            when(repository.findByBusiness_Id(businessId))
                .thenReturn(Optional.of(config));

            // When
            BigDecimal result = service.getMonthlyTolerancePercent(businessId);

            // Then
            assertThat(result).isEqualByComparingTo(configuredTolerance);
        }

        @Test
        @DisplayName("should return default 10 percent when no configuration exists")
        void shouldReturnDefaultTenPercentWhenNoConfigurationExists() {
            // Given
            Long businessId = 1L;

            when(repository.findByBusiness_Id(businessId))
                .thenReturn(Optional.empty());

            // When
            BigDecimal result = service.getMonthlyTolerancePercent(businessId);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should return exact configured value without rounding")
        void shouldReturnExactConfiguredValueWithoutRounding() {
            // Given
            Long businessId = 1L;
            BigDecimal preciseTolerance = new BigDecimal("12.50");

            BusinessConfiguration config = new BusinessConfiguration();
            config.setBusiness(testBusiness);
            config.setMonthlyHourTolerancePercent(preciseTolerance);

            when(repository.findByBusiness_Id(businessId))
                .thenReturn(Optional.of(config));

            // When
            BigDecimal result = service.getMonthlyTolerancePercent(businessId);

            // Then
            assertThat(result).isEqualByComparingTo(preciseTolerance);
        }

        @Test
        @DisplayName("should handle zero tolerance configuration")
        void shouldHandleZeroToleranceConfiguration() {
            // Given
            Long businessId = 1L;
            BigDecimal zeroTolerance = BigDecimal.ZERO;

            BusinessConfiguration config = new BusinessConfiguration();
            config.setBusiness(testBusiness);
            config.setMonthlyHourTolerancePercent(zeroTolerance);

            when(repository.findByBusiness_Id(businessId))
                .thenReturn(Optional.of(config));

            // When
            BigDecimal result = service.getMonthlyTolerancePercent(businessId);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle maximum tolerance configuration")
        void shouldHandleMaximumToleranceConfiguration() {
            // Given
            Long businessId = 1L;
            BigDecimal maxTolerance = new BigDecimal("100.00");

            BusinessConfiguration config = new BusinessConfiguration();
            config.setBusiness(testBusiness);
            config.setMonthlyHourTolerancePercent(maxTolerance);

            when(repository.findByBusiness_Id(businessId))
                .thenReturn(Optional.of(config));

            // When
            BigDecimal result = service.getMonthlyTolerancePercent(businessId);

            // Then
            assertThat(result).isEqualByComparingTo(maxTolerance);
        }
    }
}
