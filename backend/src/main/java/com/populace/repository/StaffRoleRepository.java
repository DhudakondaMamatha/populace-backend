package com.populace.repository;

import com.populace.domain.StaffMember;
import com.populace.domain.StaffRole;
import com.populace.domain.enums.EmploymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {

    List<StaffRole> findByStaff_IdAndActive(Long staffId, boolean active);

    List<StaffRole> findByStaff_Id(Long staffId);

    List<StaffRole> findByRole_IdAndActive(Long roleId, boolean active);

    Optional<StaffRole> findByStaff_IdAndRole_Id(Long staffId, Long roleId);

    @Query("SELECT sr FROM StaffRole sr WHERE sr.staff.id = :staffId " +
           "AND sr.role.id = :roleId AND sr.active = true")
    Optional<StaffRole> findActiveStaffRole(
            @Param("staffId") Long staffId,
            @Param("roleId") Long roleId);

    @Query("SELECT sr FROM StaffRole sr WHERE sr.staff.business.id = :businessId " +
           "AND sr.active = true")
    List<StaffRole> findActiveByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT sr FROM StaffRole sr " +
           "JOIN FETCH sr.role " +
           "WHERE sr.staff.id = :staffId AND sr.active = :active")
    List<StaffRole> findByStaffIdAndActiveWithRole(
            @Param("staffId") Long staffId,
            @Param("active") boolean active);

    @Query("SELECT sr FROM StaffRole sr " +
           "JOIN FETCH sr.role " +
           "WHERE sr.staff.id = :staffId AND sr.primary = true AND sr.active = true")
    Optional<StaffRole> findPrimaryRoleByStaffId(@Param("staffId") Long staffId);

    @Query("SELECT COUNT(sr) > 0 FROM StaffRole sr " +
           "WHERE sr.staff.id = :staffId AND sr.primary = true AND sr.active = true " +
           "AND sr.id != :excludeId")
    boolean existsOtherPrimaryRole(
            @Param("staffId") Long staffId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(sr) > 0 FROM StaffRole sr " +
           "WHERE sr.staff.id = :staffId AND sr.primary = true AND sr.active = true")
    boolean existsPrimaryRole(@Param("staffId") Long staffId);

    @Query("SELECT sr FROM StaffRole sr " +
           "WHERE sr.staff.id = :staffId AND sr.role.id = :roleId AND sr.active = true")
    Optional<StaffRole> findByStaffIdAndRoleIdAndIsActiveTrue(
            @Param("staffId") Long staffId,
            @Param("roleId") Long roleId);

    @Query("SELECT sr FROM StaffRole sr " +
           "WHERE sr.staff.id = :staffId AND sr.active = true")
    List<StaffRole> findByStaff_IdAndIsActiveTrue(@Param("staffId") Long staffId);
    

    @Query("SELECT sr.staff FROM StaffRole sr WHERE sr.staff.business.id = :businessId " +
            "AND sr.role.id = :roleId AND sr.active = true and sr.staff.employmentStatus = :employmentStatus")
    List<StaffMember> findByBussinessIdAndRoldId(Long businessId, Long roleId, EmploymentStatus employmentStatus);
}
