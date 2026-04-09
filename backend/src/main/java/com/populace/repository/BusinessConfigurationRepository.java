package com.populace.repository;

import com.populace.domain.BusinessConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessConfigurationRepository extends JpaRepository<BusinessConfiguration, Long> {

    Optional<BusinessConfiguration> findByBusiness_Id(Long businessId);

    boolean existsByBusiness_Id(Long businessId);
}
