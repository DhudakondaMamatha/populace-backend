package com.populace.repository;

import com.populace.domain.StaffSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffSiteRepository extends JpaRepository<StaffSite, Long> {

    List<StaffSite> findByStaff_IdAndActive(Long staffId, boolean active);

    List<StaffSite> findByStaff_Id(Long staffId);

    List<StaffSite> findBySite_IdAndActive(Long siteId, boolean active);

    Optional<StaffSite> findByStaff_IdAndSite_Id(Long staffId, Long siteId);

    @Query("SELECT ss FROM StaffSite ss WHERE ss.staff.id = :staffId " +
           "AND ss.site.id = :siteId AND ss.active = true")
    Optional<StaffSite> findActiveStaffSite(
            @Param("staffId") Long staffId,
            @Param("siteId") Long siteId);

    @Query("SELECT ss FROM StaffSite ss WHERE ss.staff.business.id = :businessId " +
           "AND ss.active = true")
    List<StaffSite> findActiveByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT ss FROM StaffSite ss " +
           "JOIN FETCH ss.site " +
           "WHERE ss.staff.id = :staffId AND ss.active = :active")
    List<StaffSite> findByStaffIdAndActiveWithSite(
            @Param("staffId") Long staffId,
            @Param("active") boolean active);

    @Query("SELECT COUNT(ss) > 0 FROM StaffSite ss " +
           "WHERE ss.staff.id = :staffId AND ss.site.id = :siteId AND ss.active = true")
    boolean existsByStaffIdAndSiteIdAndIsActiveTrue(
            @Param("staffId") Long staffId,
            @Param("siteId") Long siteId);
}
