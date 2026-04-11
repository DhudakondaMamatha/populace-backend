package com.populace.repository;

import com.populace.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findByBusiness_IdAndDeletedAtIsNull(Long businessId);

    List<Site> findByBusiness_IdAndActiveAndDeletedAtIsNull(Long businessId, boolean active);

    Optional<Site> findByIdAndBusiness_IdAndDeletedAtIsNull(Long id, Long businessId);

    Optional<Site> findByBusiness_IdAndCode(Long businessId, String code);

    boolean existsByBusiness_IdAndDeletedAtIsNull(Long businessId);

    long countByBusiness_IdAndDeletedAtIsNull(Long businessId);

    boolean existsByIdAndBusiness_Id(Long id, Long businessId);
}
