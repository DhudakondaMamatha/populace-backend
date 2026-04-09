package com.populace.repository;

import com.populace.domain.AllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRuleRepository extends JpaRepository<AllocationRule, Long> {

    List<AllocationRule> findByBusiness_IdOrderByPriorityAsc(Long businessId);

    Optional<AllocationRule> findByBusiness_IdAndRuleKey(Long businessId, String ruleKey);
}
