package com.populace.onboarding.repository;

import com.populace.onboarding.domain.BusinessOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessOnboardingRepository extends JpaRepository<BusinessOnboarding, Long> {

    Optional<BusinessOnboarding> findByBusinessId(Long businessId);

    boolean existsByBusinessIdAndIsCompleteTrue(Long businessId);
}
