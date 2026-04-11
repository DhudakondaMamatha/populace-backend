package com.populace.repository;

import com.populace.domain.ScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long> {

    List<ScheduleTemplate> findByBusiness_Id(Long businessId);

    List<ScheduleTemplate> findByBusiness_IdAndActiveTrue(Long businessId);

    Optional<ScheduleTemplate> findByIdAndBusiness_Id(Long id, Long businessId);

    boolean existsByBusiness_Id(Long businessId);
}
