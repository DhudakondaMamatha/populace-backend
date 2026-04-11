package com.populace.repository;

import com.populace.domain.ScheduleTemplateShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleTemplateShiftRepository extends JpaRepository<ScheduleTemplateShift, Long> {

    List<ScheduleTemplateShift> findByTemplate_Id(Long templateId);

    void deleteByTemplate_Id(Long templateId);
}
