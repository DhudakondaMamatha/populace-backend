package com.populace.compensation.service;

import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.dto.PayrollResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating payroll based on compensation type.
 * Handles both hourly and monthly salary calculations.
 */
@Service
public class PayrollCalculationService {

    private final StaffCompensationService compensationService;

    /**
     * Standard monthly hours used for reference calculations.
     * Based on 40 hours/week x 52 weeks / 12 months.
     */
    private static final BigDecimal STANDARD_MONTHLY_HOURS = new BigDecimal("173.33");

    /**
     * Default number of pay periods per month (semi-monthly).
     */
    private static final int DEFAULT_PAY_PERIODS_PER_MONTH = 2;

    public PayrollCalculationService(StaffCompensationService compensationService) {
        this.compensationService = compensationService;
    }

    /**
     * Calculate pay for a staff member based on hours worked.
     */
    @Transactional(readOnly = true)
    public PayrollResult calculatePay(Long staffId, BigDecimal hoursWorked) {
        return compensationService.getCurrentCompensation(staffId)
                .map(compensation -> calculatePayForCompensation(compensation, hoursWorked))
                .orElse(PayrollResult.noCompensationRecord(staffId));
    }

    /**
     * Calculate pay for a specific pay period for a salaried worker.
     */
    @Transactional(readOnly = true)
    public PayrollResult calculateMonthlySalaryPortion(Long staffId, int payPeriodsPerMonth) {
        return compensationService.getCurrentCompensation(staffId)
                .filter(CompensationDto::isMonthly)
                .map(compensation -> calculateSalaryPortion(compensation, payPeriodsPerMonth))
                .orElse(PayrollResult.notSalaried(staffId));
    }

    private PayrollResult calculatePayForCompensation(
            CompensationDto compensation,
            BigDecimal hoursWorked
    ) {
        if (compensation.isHourly()) {
            return calculateHourlyPay(compensation, hoursWorked);
        } else {
            return calculateSalaryPortion(compensation, DEFAULT_PAY_PERIODS_PER_MONTH);
        }
    }

    /**
     * Calculate pay for an hourly worker.
     */
    private PayrollResult calculateHourlyPay(CompensationDto compensation, BigDecimal hoursWorked) {
        BigDecimal hourlyRate = compensation.hourlyRate();
        BigDecimal totalPay = hoursWorked.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);

        return new PayrollResult(
                compensation.staffId(),
                "hourly",
                hoursWorked,
                totalPay,
                null
        );
    }

    /**
     * Calculate a salary portion for a monthly-salaried worker.
     */
    private PayrollResult calculateSalaryPortion(CompensationDto compensation, int payPeriodsPerMonth) {
        BigDecimal monthlySalary = compensation.monthlySalary();
        if (monthlySalary == null) {
            return PayrollResult.noSalaryConfigured(compensation.staffId());
        }

        BigDecimal portionAmount = monthlySalary.divide(
                BigDecimal.valueOf(payPeriodsPerMonth), 2, RoundingMode.HALF_UP);

        return new PayrollResult(
                compensation.staffId(),
                "monthly",
                null,
                portionAmount,
                "Salary portion: 1/" + payPeriodsPerMonth + " of monthly salary"
        );
    }

    /**
     * Get an estimated hourly rate for a salaried worker.
     */
    @Transactional(readOnly = true)
    public BigDecimal getEstimatedHourlyRateForSalaried(Long staffId) {
        return compensationService.getCurrentCompensation(staffId)
                .filter(CompensationDto::isMonthly)
                .map(comp -> {
                    if (comp.monthlySalary() == null) {
                        return null;
                    }
                    return comp.monthlySalary().divide(STANDARD_MONTHLY_HOURS, 2, RoundingMode.HALF_UP);
                })
                .orElse(null);
    }

    /**
     * Get the effective rate for cost estimation.
     */
    @Transactional(readOnly = true)
    public BigDecimal getEffectiveRateForCostEstimation(Long staffId) {
        return compensationService.getCurrentCompensation(staffId)
                .map(comp -> {
                    if (comp.isHourly()) {
                        return comp.hourlyRate();
                    } else if (comp.monthlySalary() != null) {
                        return comp.monthlySalary().divide(STANDARD_MONTHLY_HOURS, 2, RoundingMode.HALF_UP);
                    }
                    return null;
                })
                .orElse(null);
    }
}
