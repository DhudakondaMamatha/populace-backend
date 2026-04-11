package com.populace.repository;

import com.populace.domain.LeaveRequest;
import com.populace.domain.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.business.id = :businessId " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStaffId(@Param("staffId") Long staffId);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId AND lr.status = :status " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStaffIdAndStatus(
        @Param("staffId") Long staffId,
        @Param("status") LeaveRequestStatus status);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId " +
           "AND EXTRACT(YEAR FROM lr.startDate) = :year " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStaffIdAndYear(
        @Param("staffId") Long staffId,
        @Param("year") Integer year);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId " +
           "AND lr.status = :status " +
           "AND EXTRACT(YEAR FROM lr.startDate) = :year " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStaffIdAndStatusAndYear(
        @Param("staffId") Long staffId,
        @Param("status") LeaveRequestStatus status,
        @Param("year") Integer year);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId AND lr.status = :status " +
           "AND lr.startDate <= :date AND lr.endDate >= :date")
    List<LeaveRequest> findByStaffAndStatusAndDateOverlap(
        @Param("staffId") Long staffId,
        @Param("status") LeaveRequestStatus status,
        @Param("date") LocalDate date);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId " +
           "AND lr.status = 'approved' " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findApprovedOverlapping(
        @Param("staffId") Long staffId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.id = :id AND lr.staff.business.id = :businessId")
    Optional<LeaveRequest> findByIdAndBusinessId(
        @Param("id") Long id, @Param("businessId") Long businessId);

    @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr " +
           "WHERE lr.staff.id = :staffId " +
           "AND lr.status = :status " +
           "AND lr.startDate <= :date AND lr.endDate >= :date")
    boolean existsApprovedLeaveOnDate(
        @Param("staffId") Long staffId,
        @Param("date") LocalDate date,
        @Param("status") LeaveRequestStatus status);

}
