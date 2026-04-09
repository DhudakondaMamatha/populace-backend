package com.populace.repository;

import com.populace.domain.StaffWorkParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffWorkParametersRepository extends JpaRepository<StaffWorkParameters, Long> {

    List<StaffWorkParameters> findByStaff_Id(Long staffId);

    @Query("""
        SELECT swp FROM StaffWorkParameters swp
        WHERE swp.staff.id = :staffId
        AND swp.effectiveFrom <= :date
        AND (swp.effectiveTo IS NULL OR swp.effectiveTo >= :date)
        ORDER BY swp.effectiveFrom DESC
        """)
    List<StaffWorkParameters> findActiveByStaffIdAndDate(Long staffId, LocalDate date);

    default Optional<StaffWorkParameters> findCurrentByStaffId(Long staffId) {
        List<StaffWorkParameters> results = findActiveByStaffIdAndDate(staffId, LocalDate.now());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("""
        SELECT swp FROM StaffWorkParameters swp
        JOIN swp.staff s
        WHERE s.business.id = :businessId
        AND s.deletedAt IS NULL
        AND s.employmentStatus = 'active'
        AND swp.effectiveFrom <= :date
        AND (swp.effectiveTo IS NULL OR swp.effectiveTo >= :date)
        ORDER BY swp.effectiveFrom DESC
        """)
    List<StaffWorkParameters> findActiveByBusinessIdAndDate(Long businessId, LocalDate date);

    /**
     * Returns the SUM of min_hours_per_day and max_hours_per_day for the given staff IDs,
     * filtered to those who hold the given role and have work parameters effective on shiftDate.
     * Result: Object[0] = totalMinHours (BigDecimal), Object[1] = totalMaxHours (BigDecimal)
     */
    @Query("""
        SELECT COALESCE(SUM(swp.minHoursPerDay), 0), COALESCE(SUM(swp.maxHoursPerDay), 0)
        FROM StaffWorkParameters swp
        JOIN swp.staff s
        JOIN StaffRole sr ON sr.staff.id = s.id
        WHERE s.id IN :staffIds
        AND sr.role.id = :roleId
        AND sr.active = true
        AND s.deletedAt IS NULL
        AND s.employmentStatus = 'active'
        AND swp.effectiveFrom <= :shiftDate
        AND (swp.effectiveTo IS NULL OR swp.effectiveTo >= :shiftDate)
        """)
    Object[] sumHoursByRoleAndDate(@Param("staffIds") List<Long> staffIds, @Param("roleId") Long roleId, @Param("shiftDate") LocalDate shiftDate);
}
