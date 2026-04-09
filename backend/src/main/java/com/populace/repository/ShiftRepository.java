package com.populace.repository;

import com.populace.domain.Shift;
import com.populace.domain.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> {

    List<Shift> findByBusiness_IdAndShiftDateBetween(
            Long businessId, LocalDate startDate, LocalDate endDate);

    List<Shift> findBySite_IdAndShiftDateBetween(
            Long siteId, LocalDate startDate, LocalDate endDate);

    List<Shift> findByBusiness_IdAndShiftDateBetweenAndStatus(
            Long businessId, LocalDate startDate, LocalDate endDate, ShiftStatus status);

    Optional<Shift> findByIdAndBusiness_Id(Long id, Long businessId);

    /**
     * Find shifts with any of the given statuses in a date range.
     * Uses parameterized enum values instead of string literals.
     */
    @Query("SELECT DISTINCT s FROM Shift s " +
           "JOIN FETCH s.site " +
           "JOIN FETCH s.role " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate BETWEEN :startDate AND :endDate " +
           "AND s.status IN :statuses " +
           "ORDER BY s.shiftDate, s.startTime")
    List<Shift> findShiftsByStatuses(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ShiftStatus> statuses);

    /**
     * Find unfilled shifts (open or partially filled) in a date range.
     * Delegates to findShiftsByStatuses with the correct enum values.
     */
    default List<Shift> findUnfilledShifts(Long businessId, LocalDate startDate, LocalDate endDate) {
        List<ShiftStatus> unfilledStatuses = List.of(ShiftStatus.open, ShiftStatus.partially_filled);
        return findShiftsByStatuses(businessId, startDate, endDate, unfilledStatuses);
    }

    @Query("SELECT s FROM Shift s " +
           "JOIN FETCH s.site " +
           "JOIN FETCH s.role " +
           "WHERE s.site.id = :siteId " +
           "AND s.shiftDate = :date " +
           "AND s.status IN :statuses " +
           "ORDER BY s.startTime")
    List<Shift> findUnfilledShiftsBySiteAndDate(
            @Param("siteId") Long siteId,
            @Param("date") LocalDate date,
            @Param("statuses") List<ShiftStatus> statuses);

    @Query("SELECT s FROM Shift s WHERE s.site.id = :siteId " +
           "AND s.role.id = :roleId AND s.shiftDate = :date")
    List<Shift> findBySiteAndRoleAndDate(
            @Param("siteId") Long siteId,
            @Param("roleId") Long roleId,
            @Param("date") LocalDate date);

    boolean existsByBusiness_Id(Long businessId);

    long countByBusiness_Id(Long businessId);

    /**
     * Find all shifts in a date range, excluding a specific status.
     * Uses parameterized enum value instead of string literal.
     */
    @Query("SELECT DISTINCT s FROM Shift s " +
           "JOIN FETCH s.site " +
           "JOIN FETCH s.role " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate BETWEEN :startDate AND :endDate " +
           "AND s.status != :excludedStatus " +
           "ORDER BY s.shiftDate, s.startTime")
    List<Shift> findAllShiftsInRangeExcludingStatus(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatus") ShiftStatus excludedStatus);

    /**
     * Find all shifts in a date range excluding cancelled shifts.
     * Used for bulk re-allocation.
     */
    default List<Shift> findAllShiftsInRange(Long businessId, LocalDate startDate, LocalDate endDate) {
        return findAllShiftsInRangeExcludingStatus(businessId, startDate, endDate, ShiftStatus.cancelled);
    }

    /**
     * Count shifts by status in a date range, excluding a specific status.
     * Uses parameterized enum value instead of string literal.
     */
    @Query("SELECT s.status, COUNT(s) FROM Shift s " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate BETWEEN :startDate AND :endDate " +
           "AND s.status != :excludedStatus " +
           "GROUP BY s.status")
    List<Object[]> countShiftsByStatusInRangeExcludingStatus(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatus") ShiftStatus excludedStatus);

    /**
     * Count shifts by status in a date range, excluding cancelled.
     */
    default List<Object[]> countShiftsByStatusInRange(Long businessId, LocalDate startDate, LocalDate endDate) {
        return countShiftsByStatusInRangeExcludingStatus(businessId, startDate, endDate, ShiftStatus.cancelled);
    }
}
