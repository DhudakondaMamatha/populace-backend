package com.populace.repository;

import com.populace.domain.WorkParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkParametersRepository extends JpaRepository<WorkParameters, Long> {

    Optional<WorkParameters> findByBusiness_IdAndIsDefaultTrue(Long businessId);

    Optional<WorkParameters> findByBusiness_IdAndActiveTrue(Long businessId);
}
