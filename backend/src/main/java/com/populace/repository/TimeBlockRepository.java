package com.populace.repository;

import com.populace.domain.TimeBlock;
import com.populace.domain.enums.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    List<TimeBlock> findByShift_Id(Long shiftId);

    List<TimeBlock> findByStaff_Id(Long staffId);

    List<TimeBlock> findByStaff_IdAndBlockType(Long staffId, BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.staff.id = :staffId " +
           "AND DATE(tb.startTime) = :date ORDER BY tb.startTime")
    List<TimeBlock> findByStaffIdAndDate(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.staff.id = :staffId " +
           "AND tb.startTime >= :rangeStart AND tb.endTime <= :rangeEnd " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findByStaffIdAndTimeRange(
            @Param("staffId") Long staffId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.site.id = :siteId " +
           "AND tb.role.id = :roleId " +
           "AND tb.startTime < :endTime AND tb.endTime > :startTime " +
           "AND tb.blockType = 'work' " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findOverlappingWorkBlocks(
            @Param("siteId") Long siteId,
            @Param("roleId") Long roleId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.staff.id = :staffId " +
           "AND tb.startTime < :endTime AND tb.endTime > :startTime " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findOverlappingBlocksForStaff(
            @Param("staffId") Long staffId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT COALESCE(SUM(tb.durationMinutes), 0) FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND DATE(tb.startTime) = :date")
    Integer sumWorkMinutesByStaffAndDate(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date);

    @Query("SELECT COUNT(tb) FROM TimeBlock tb WHERE tb.staff.id = :staffId " +
           "AND tb.startTime < :endTime AND tb.endTime > :startTime")
    long countOverlappingBlocks(
            @Param("staffId") Long staffId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.shift.id = :shiftId " +
           "AND tb.blockType = :blockType ORDER BY tb.startTime")
    List<TimeBlock> findByShiftIdAndBlockType(
            @Param("shiftId") Long shiftId,
            @Param("blockType") BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.shift.id = :shiftId " +
           "AND tb.staff.id = :staffId AND tb.blockType = :blockType ORDER BY tb.startTime")
    List<TimeBlock> findByShiftIdAndStaffIdAndBlockType(
            @Param("shiftId") Long shiftId,
            @Param("staffId") Long staffId,
            @Param("blockType") BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.site.id = :siteId " +
           "AND tb.startTime < :endTime AND tb.endTime > :startTime " +
           "AND tb.blockType = :blockType " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findBySiteAndTimeRangeAndBlockType(
            @Param("siteId") Long siteId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("blockType") BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.role.id = :roleId " +
           "AND tb.startTime < :endTime AND tb.endTime > :startTime " +
           "AND tb.blockType = :blockType " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findByRoleAndTimeRangeAndBlockType(
            @Param("roleId") Long roleId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("blockType") BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb " +
           "WHERE tb.startTime < :endTime AND tb.endTime > :startTime " +
           "AND tb.blockType = :blockType " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findByTimeRangeAndBlockType(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("blockType") BlockType blockType);

    @Query("SELECT tb FROM TimeBlock tb " +
           "JOIN FETCH tb.shift s " +
           "JOIN FETCH s.role " +
           "WHERE tb.staff.id = :staffId " +
           "AND DATE(tb.startTime) >= :startDate " +
           "AND DATE(tb.startTime) <= :endDate " +
           "AND tb.blockType = 'work' " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findWorkBlocksByStaffIdAndDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT tb FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND DATE(tb.startTime) >= :startDate " +
           "AND DATE(tb.startTime) <= :endDate " +
           "ORDER BY tb.startTime")
    List<TimeBlock> findActiveBlocksInDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all time blocks for a staff member in a date range.
     */
    @Query("SELECT tb FROM TimeBlock tb " +
           "JOIN FETCH tb.shift s " +
           "WHERE tb.staff.id = :staffId " +
           "AND s.shiftDate >= :startDate " +
           "AND s.shiftDate <= :endDate " +
           "ORDER BY s.shiftDate, tb.startTime")
    List<TimeBlock> findByStaffIdAndDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all work blocks for a business within a date range.
     * Used for reporting.
     */
    @Query("SELECT tb FROM TimeBlock tb " +
           "JOIN FETCH tb.staff s " +
           "WHERE s.business.id = :businessId " +
           "AND DATE(tb.startTime) >= :startDate " +
           "AND DATE(tb.startTime) <= :endDate " +
           "AND tb.blockType = 'work' " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.lastName, s.firstName, tb.startTime")
    List<TimeBlock> findWorkBlocksByBusinessIdAndDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum total work minutes for a staff member in a year.
     * Used for leave summary reports.
     */
    @Query("SELECT COALESCE(SUM(tb.durationMinutes), 0) FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND EXTRACT(YEAR FROM tb.startTime) = :year")
    Integer sumWorkMinutesByStaffAndYear(
            @Param("staffId") Long staffId,
            @Param("year") int year);

    /**
     * Count manual allocations for a business in a date range.
     * Manual allocations have createdBy = 'MANUAL' or 'MANUAL_ALLOCATION'.
     */
    @Query("SELECT COUNT(tb) FROM TimeBlock tb " +
           "JOIN tb.shift s " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate >= :startDate " +
           "AND s.shiftDate <= :endDate " +
           "AND tb.blockType = 'work' " +
           "AND UPPER(tb.createdBy) IN ('MANUAL', 'MANUAL_ALLOCATION')")
    long countManualAllocationsInRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count automatic allocations for a business in a date range.
     * Auto allocations have createdBy NOT 'MANUAL' or NULL.
     */
    @Query("SELECT COUNT(tb) FROM TimeBlock tb " +
           "JOIN tb.shift s " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate >= :startDate " +
           "AND s.shiftDate <= :endDate " +
           "AND tb.blockType = 'work' " +
           "AND (tb.createdBy IS NULL OR UPPER(tb.createdBy) NOT IN ('MANUAL', 'MANUAL_ALLOCATION'))")
    long countAutoAllocationsInRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Return the most recent calendar date on which the staff member had a work block
     * strictly before the given shift date. Returns null if no prior work blocks exist.
     * Used for scoring candidates by rest time.
     */
    @Query("SELECT MAX(tb.startTime) FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND DATE(tb.startTime) < :shiftDate")
    Instant findLastWorkInstantBefore(
            @Param("staffId") Long staffId,
            @Param("shiftDate") LocalDate shiftDate);

    /**
     * Count distinct calendar days a staff member has worked work blocks in a week.
     * Used for weekly off-rotation enforcement.
     */
    @Query("SELECT COUNT(DISTINCT DATE(tb.startTime)) FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND DATE(tb.startTime) >= :weekStart " +
           "AND DATE(tb.startTime) <= :weekEnd")
    int countWorkedDaysByStaffAndWeek(
            @Param("staffId") Long staffId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    /**
     * Sum total work minutes for a staff member in a calendar month.
     * Falls back gracefully when durationMinutes is null (uses start/end difference).
     */
    @Query("SELECT COALESCE(SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, tb.startTime, tb.endTime)), 0) " +
           "FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND DATE(tb.startTime) >= :monthStart " +
           "AND DATE(tb.startTime) <= :monthEnd")
    long sumWorkMinutesByStaffAndMonth(
            @Param("staffId") Long staffId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * Sum total work minutes for a staff member in a given week.
     * Falls back gracefully when durationMinutes is null (uses start/end difference).
     */
    @Query("SELECT COALESCE(SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, tb.startTime, tb.endTime)), 0) " +
           "FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'work' " +
           "AND DATE(tb.startTime) >= :weekStart " +
           "AND DATE(tb.startTime) <= :weekEnd")
    long sumWorkMinutesByStaffAndWeek(
            @Param("staffId") Long staffId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    @Query("SELECT COALESCE(SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, tb.startTime, tb.endTime)), 0) " +
           "FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'break_period' " +
           "AND DATE(tb.startTime) = :date")
    long sumBreakMinutesByStaffAndDate(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, tb.startTime, tb.endTime)), 0) " +
           "FROM TimeBlock tb " +
           "WHERE tb.staff.id = :staffId " +
           "AND tb.blockType = 'break_period' " +
           "AND DATE(tb.startTime) >= :weekStart " +
           "AND DATE(tb.startTime) <= :weekEnd")
    long sumBreakMinutesByStaffAndWeek(
            @Param("staffId") Long staffId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    /**
     * Find all time blocks for shifts in a date range (for bulk operations).
     */
    @Query("SELECT tb FROM TimeBlock tb " +
           "JOIN FETCH tb.shift s " +
           "WHERE s.business.id = :businessId " +
           "AND s.shiftDate >= :startDate " +
           "AND s.shiftDate <= :endDate " +
           "AND s.status != 'cancelled' " +
           "ORDER BY s.shiftDate, tb.startTime")
    List<TimeBlock> findAllBlocksInDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    List<TimeBlock> findByStaff_IdAndRole_Id(Long staffId, Long roleId);

    List<TimeBlock> findByShift_IdAndRole_Id(Long shiftId, Long roleId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM TimeBlock tb WHERE tb.shift.id IN " +
           "(SELECT s.id FROM Shift s WHERE s.business.id = :businessId " +
           "AND s.shiftDate >= :startDate AND s.shiftDate <= :endDate) " +
           "AND (tb.createdBy IS NULL OR UPPER(tb.createdBy) NOT IN ('MANUAL', 'MANUAL_ALLOCATION'))")
    int deleteAutoAllocationsInRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
