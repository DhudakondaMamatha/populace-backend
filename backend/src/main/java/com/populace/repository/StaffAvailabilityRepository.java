package com.populace.repository;

import com.populace.domain.StaffAvailability;
import com.populace.domain.enums.AvailabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailability, Long> {

    List<StaffAvailability> findByStaff_Id(Long staffId);

    @Query("""
        SELECT sa FROM StaffAvailability sa
        WHERE sa.staff.id = :staffId
        AND (sa.effectiveFrom IS NULL OR sa.effectiveFrom <= :date)
        AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date)
        """)
    List<StaffAvailability> findActiveByStaffIdAndDate(
        @Param("staffId") Long staffId,
        @Param("date") LocalDate date);

    /**
     * Check if staff has an unavailable entry for a specific date.
     * Checks specific_date match only (day_of_week matching done in service layer).
     */
    @Query("""
        SELECT COUNT(sa) > 0 FROM StaffAvailability sa
        WHERE sa.staff.id = :staffId
        AND sa.availabilityType = :type
        AND (
            sa.specificDate = :date
            OR (sa.specificDate IS NULL
                AND sa.dayOfWeek = :dayOfWeek
                AND (sa.effectiveFrom IS NULL OR sa.effectiveFrom <= :date)
                AND (sa.effectiveTo IS NULL OR sa.effectiveTo >= :date))
        )
        """)
    boolean existsUnavailableOnDate(
        @Param("staffId") Long staffId,
        @Param("date") LocalDate date,
        @Param("dayOfWeek") Short dayOfWeek,
        @Param("type") AvailabilityType type);
}
