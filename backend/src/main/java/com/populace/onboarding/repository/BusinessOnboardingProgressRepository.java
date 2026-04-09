package com.populace.onboarding.repository;

import com.populace.onboarding.domain.BusinessOnboardingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessOnboardingProgressRepository extends JpaRepository<BusinessOnboardingProgress, Long> {

    List<BusinessOnboardingProgress> findByBusinessId(Long businessId);

    Optional<BusinessOnboardingProgress> findByBusinessIdAndStageId(Long businessId, Long stageId);

    boolean existsByBusinessIdAndStageId(Long businessId, Long stageId);

    long countByBusinessIdAndStatus(Long businessId, String status);

    void deleteByBusinessId(Long businessId);
}
