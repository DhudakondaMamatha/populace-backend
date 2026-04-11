package com.populace.repository;

import com.populace.domain.StaffCompensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffCompensationRepository extends JpaRepository<StaffCompensation, Long> {

    List<StaffCompensation> findByStaff_Id(Long staffId);

    List<StaffCompensation> findByStaff_IdAndRole_Id(Long staffId, Long roleId);

    @Query("SELECT sc FROM StaffCompensation sc " +
           "WHERE sc.staff.id = :staffId " +
           "AND sc.effectiveFrom <= :date " +
           "AND (sc.effectiveTo IS NULL OR sc.effectiveTo >= :date) " +
           "ORDER BY sc.effectiveFrom DESC")
    List<StaffCompensation> findActiveByStaffIdAndDate(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date);

    @Query("SELECT sc FROM StaffCompensation sc " +
           "WHERE sc.staff.id = :staffId " +
           "AND sc.role.id = :roleId " +
           "AND sc.effectiveFrom <= :date " +
           "AND (sc.effectiveTo IS NULL OR sc.effectiveTo >= :date) " +
           "ORDER BY sc.effectiveFrom DESC")
    Optional<StaffCompensation> findActiveByStaffIdAndRoleIdAndDate(
            @Param("staffId") Long staffId,
            @Param("roleId") Long roleId,
            @Param("date") LocalDate date);

    @Query("SELECT sc.hourlyRate FROM StaffCompensation sc " +
           "WHERE sc.staff.id = :staffId " +
           "AND (sc.role.id = :roleId OR :roleId IS NULL) " +
           "AND sc.effectiveFrom <= :date " +
           "AND (sc.effectiveTo IS NULL OR sc.effectiveTo >= :date) " +
           "ORDER BY CASE WHEN sc.role.id = :roleId THEN 0 ELSE 1 END, sc.effectiveFrom DESC")
    Optional<BigDecimal> findCurrentRate(
            @Param("staffId") Long staffId,
            @Param("roleId") Long roleId,
            @Param("date") LocalDate date);

    Optional<StaffCompensation> findTopByStaff_IdOrderByEffectiveFromDesc(Long staffId);

    /**
     * Find the currently active compensation for a staff member.
     * Active means effectiveTo is null (open-ended record).
     */
    @Query("SELECT c FROM StaffCompensation c WHERE c.staff.id = :staffId " +
           "AND c.effectiveTo IS NULL ORDER BY c.effectiveFrom DESC")
    Optional<StaffCompensation> findActiveByStaffId(@Param("staffId") Long staffId);
}
