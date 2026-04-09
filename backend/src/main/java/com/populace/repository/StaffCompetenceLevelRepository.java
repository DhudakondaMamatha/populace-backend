package com.populace.repository;

import com.populace.domain.StaffCompetenceLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffCompetenceLevelRepository extends JpaRepository<StaffCompetenceLevel, Long> {

    List<StaffCompetenceLevel> findByStaff_Id(Long staffId);

    @Modifying
    @Query("DELETE FROM StaffCompetenceLevel scl WHERE scl.staff.id = :staffId")
    void deleteAllByStaffId(Long staffId);

    boolean existsByStaff_IdAndLevel(Long staffId, String level);
}
