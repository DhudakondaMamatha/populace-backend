package com.populace.compensation.service;

import com.populace.compensation.dto.CompensationSummary;
import com.populace.domain.StaffCompensation;
import com.populace.domain.enums.CompensationType;
import com.populace.repository.StaffCompensationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Read-only service for compensation reporting and analytics.
 * Provides aggregated data for business reports.
 */
@Service
public class CompensationReportingService {

    private final StaffCompensationRepository compensationRepository;
    private final JdbcTemplate jdbcTemplate;

    public CompensationReportingService(
            StaffCompensationRepository compensationRepository,
            JdbcTemplate jdbcTemplate) {
        this.compensationRepository = compensationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get a summary of compensation by type for a business.
     *
     * @param businessId the business ID
     * @return compensation summary with counts and totals
     */
    @Transactional(readOnly = true)
    public CompensationSummary getCompensationSummary(Long businessId) {
        String sql = """
            SELECT
                sc.compensation_type,
                COUNT(DISTINCT sc.staff_id) as staff_count,
                SUM(CASE WHEN sc.compensation_type = 'monthly' THEN sc.monthly_salary ELSE 0 END) as total_monthly_salary,
                AVG(sc.hourly_rate) as avg_hourly_rate
            FROM staff_compensation sc
            JOIN staff_members sm ON sc.staff_id = sm.id
            WHERE sm.business_id = ?
              AND sm.deleted_at IS NULL
              AND sc.effective_from <= CURRENT_DATE
              AND (sc.effective_to IS NULL OR sc.effective_to >= CURRENT_DATE)
            GROUP BY sc.compensation_type
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, businessId);

        int hourlyCount = 0;
        int monthlyCount = 0;
        BigDecimal totalMonthlySalary = BigDecimal.ZERO;
        BigDecimal avgHourlyRate = BigDecimal.ZERO;

        for (Map<String, Object> row : rows) {
            String type = (String) row.get("compensation_type");
            int count = ((Number) row.get("staff_count")).intValue();

            if ("hourly".equals(type)) {
                hourlyCount = count;
                Object avgRate = row.get("avg_hourly_rate");
                if (avgRate != null) {
                    avgHourlyRate = new BigDecimal(avgRate.toString());
                }
            } else if ("monthly".equals(type)) {
                monthlyCount = count;
                Object totalSalary = row.get("total_monthly_salary");
                if (totalSalary != null) {
                    totalMonthlySalary = new BigDecimal(totalSalary.toString());
                }
            }
        }

        return new CompensationSummary(
                businessId,
                hourlyCount,
                monthlyCount,
                hourlyCount + monthlyCount,
                avgHourlyRate,
                totalMonthlySalary
        );
    }

    /**
     * Count staff by compensation type for a business.
     *
     * @param businessId the business ID
     * @param type the compensation type to count
     * @return number of staff with that compensation type
     */
    @Transactional(readOnly = true)
    public int countStaffByCompensationType(Long businessId, CompensationType type) {
        String sql = """
            SELECT COUNT(DISTINCT sc.staff_id)
            FROM staff_compensation sc
            JOIN staff_members sm ON sc.staff_id = sm.id
            WHERE sm.business_id = ?
              AND sm.deleted_at IS NULL
              AND sc.compensation_type = ?
              AND sc.effective_from <= CURRENT_DATE
              AND (sc.effective_to IS NULL OR sc.effective_to >= CURRENT_DATE)
            """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, businessId, type.name());
        return count != null ? count : 0;
    }

    /**
     * Get total monthly salary expense for a business.
     *
     * @param businessId the business ID
     * @return total monthly salary for all salaried employees
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalMonthlySalaryExpense(Long businessId) {
        String sql = """
            SELECT COALESCE(SUM(sc.monthly_salary), 0)
            FROM staff_compensation sc
            JOIN staff_members sm ON sc.staff_id = sm.id
            WHERE sm.business_id = ?
              AND sm.deleted_at IS NULL
              AND sc.compensation_type = 'monthly'
              AND sc.effective_from <= CURRENT_DATE
              AND (sc.effective_to IS NULL OR sc.effective_to >= CURRENT_DATE)
            """;

        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class, businessId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get the average hourly rate for hourly workers in a business.
     *
     * @param businessId the business ID
     * @return average hourly rate
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageHourlyRate(Long businessId) {
        String sql = """
            SELECT COALESCE(AVG(sc.hourly_rate), 0)
            FROM staff_compensation sc
            JOIN staff_members sm ON sc.staff_id = sm.id
            WHERE sm.business_id = ?
              AND sm.deleted_at IS NULL
              AND sc.compensation_type = 'hourly'
              AND sc.effective_from <= CURRENT_DATE
              AND (sc.effective_to IS NULL OR sc.effective_to >= CURRENT_DATE)
            """;

        BigDecimal avg = jdbcTemplate.queryForObject(sql, BigDecimal.class, businessId);
        return avg != null ? avg : BigDecimal.ZERO;
    }

    /**
     * Get compensation records expiring within a date range.
     * Useful for alerts about contracts needing renewal.
     *
     * @param businessId the business ID
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return list of compensation records expiring in the range
     */
    @Transactional(readOnly = true)
    public List<StaffCompensation> getExpiringCompensation(
            Long businessId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT sc.* FROM staff_compensation sc
            JOIN staff_members sm ON sc.staff_id = sm.id
            WHERE sm.business_id = ?
              AND sm.deleted_at IS NULL
              AND sc.effective_to BETWEEN ? AND ?
            ORDER BY sc.effective_to
            """;

        // Note: This returns entities for simplicity; in production, consider returning DTOs
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            // Simplified mapping - in practice, use a proper row mapper
            return compensationRepository.findById(rs.getLong("id")).orElse(null);
        }, businessId, startDate, endDate).stream()
                .filter(c -> c != null)
                .toList();
    }
}
