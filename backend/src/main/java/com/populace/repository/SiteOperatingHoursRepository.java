package com.populace.repository;

import com.populace.domain.SiteOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteOperatingHoursRepository extends JpaRepository<SiteOperatingHours, Long> {

    List<SiteOperatingHours> findBySite_IdOrderByDayOfWeek(Long siteId);

    Optional<SiteOperatingHours> findBySite_IdAndDayOfWeek(Long siteId, Integer dayOfWeek);

    void deleteBySite_Id(Long siteId);

    boolean existsBySite_Id(Long siteId);
}
