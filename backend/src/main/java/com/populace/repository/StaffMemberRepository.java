package com.populace.repository;

import com.populace.domain.StaffMember;
import com.populace.domain.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    List<StaffMember> findByBusiness_IdAndDeletedAtIsNull(Long businessId);

    List<StaffMember> findByBusiness_IdAndEmploymentStatusAndDeletedAtIsNull(
            Long businessId, EmploymentStatus status);

    Optional<StaffMember> findByIdAndBusiness_IdAndDeletedAtIsNull(Long id, Long businessId);

    Optional<StaffMember> findByBusiness_IdAndEmployeeCode(Long businessId, String employeeCode);

    @Query("SELECT s FROM StaffMember s WHERE s.business.id = :businessId " +
           "AND s.employmentStatus = 'active' AND s.deletedAt IS NULL")
    List<StaffMember> findActiveStaff(@Param("businessId") Long businessId);

    boolean existsByBusiness_IdAndDeletedAtIsNull(Long businessId);

    long countByBusiness_IdAndDeletedAtIsNull(Long businessId);

    boolean existsByEmailIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(String email, Long businessId);

    boolean existsByEmployeeCodeIgnoreCaseAndBusiness_IdAndDeletedAtIsNull(String employeeCode, Long businessId);

    @Query("SELECT s FROM StaffMember s WHERE s.business.id = :businessId " +
           "AND s.employmentStatus = :status AND s.deletedAt IS NULL")
    List<StaffMember> findByBusinessIdAndEmploymentStatus(
            @Param("businessId") Long businessId,
            @Param("status") String status);

    /**
     * Find the current version of a staff member for optimistic locking.
     */
    @Query("SELECT s.version FROM StaffMember s WHERE s.id = :id")
    Long findVersionById(@Param("id") Long id);
}
