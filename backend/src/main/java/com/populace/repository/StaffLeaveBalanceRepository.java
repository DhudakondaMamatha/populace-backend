package com.populace.repository;

import com.populace.domain.StaffLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffLeaveBalanceRepository extends JpaRepository<StaffLeaveBalance, Long> {

    @Query("SELECT slb FROM StaffLeaveBalance slb " +
           "WHERE slb.staff.id = :staffId AND slb.year = :year")
    List<StaffLeaveBalance> findByStaffIdAndYear(
        @Param("staffId") Long staffId, @Param("year") Integer year);

    @Query("SELECT slb FROM StaffLeaveBalance slb " +
           "WHERE slb.staff.id = :staffId " +
           "AND slb.leaveType.id = :leaveTypeId " +
           "AND slb.year = :year")
    Optional<StaffLeaveBalance> findByStaffAndLeaveTypeAndYear(
        @Param("staffId") Long staffId,
        @Param("leaveTypeId") Long leaveTypeId,
        @Param("year") Integer year);
}
