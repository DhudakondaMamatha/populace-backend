package com.populace.onboarding.repository;

import com.populace.onboarding.domain.OnboardingStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingStageRepository extends JpaRepository<OnboardingStage, Long> {

    List<OnboardingStage> findAllByOrderByDisplayOrderAsc();

    Optional<OnboardingStage> findByCode(String code);

    List<OnboardingStage> findByIsRequiredTrueOrderByDisplayOrderAsc();

    long countByIsRequiredTrue();
}
