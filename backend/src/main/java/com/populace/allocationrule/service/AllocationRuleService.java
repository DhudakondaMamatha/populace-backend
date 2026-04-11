package com.populace.allocationrule.service;

import com.populace.allocationrule.dto.AllocationRuleDto;
import com.populace.allocationrule.dto.AllocationRulesUpdateRequest;
import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.AllocationRule;
import com.populace.domain.Business;
import com.populace.repository.AllocationRuleRepository;
import com.populace.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AllocationRuleService {

    private static final Set<String> SORTING_RULES = Set.of("cheapest", "expertise", "hours_available");

    private static final Map<String, String> RULE_LABELS = Map.of(
        "cheapest",           "Catch by cheapest in position",
        "expertise",          "Catch by expertise level in position",
        "hours_available",    "Catch by most hours available",
        "partial_allocation", "Enable partial shift allocation",
        "role_combos",        "Enable role combo cross-qualification"
    );

    private final AllocationRuleRepository allocationRuleRepository;
    private final BusinessRepository businessRepository;

    public AllocationRuleService(AllocationRuleRepository allocationRuleRepository,
                                 BusinessRepository businessRepository) {
        this.allocationRuleRepository = allocationRuleRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public List<AllocationRuleDto> getRules(Long businessId) {
        List<AllocationRule> rules = allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId);

        // If no rules exist yet (new business), seed defaults
        if (rules.isEmpty()) {
            rules = seedDefaults(businessId);
        }

        return rules.stream().map(this::toDto).toList();
    }

    @Transactional
    public List<AllocationRuleDto> updateRules(Long businessId, AllocationRulesUpdateRequest request) {
        // Enforce: exactly one sorting rule must be enabled
        long enabledSortingCount = request.rules().stream()
            .filter(e -> SORTING_RULES.contains(e.ruleKey()) && e.enabled())
            .count();
        if (enabledSortingCount != 1) {
            throw new IllegalArgumentException("Exactly one sorting rule must be enabled (cheapest, expertise, or hours_available)");
        }

        for (AllocationRulesUpdateRequest.RuleEntry entry : request.rules()) {
            AllocationRule rule = allocationRuleRepository
                .findByBusiness_IdAndRuleKey(businessId, entry.ruleKey())
                .orElseThrow(() -> new ResourceNotFoundException("AllocationRule not found: " + entry.ruleKey()));

            rule.setEnabled(entry.enabled());
            rule.setPriority(entry.priority());
            allocationRuleRepository.save(rule);
        }

        return allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId)
            .stream().map(this::toDto).toList();
    }

    private List<AllocationRule> seedDefaults(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        String[][] defaults = {
            {"cheapest",           "true"},   // sorting: enabled by default
            {"expertise",          "false"},  // sorting: user enables via UI
            {"hours_available",    "false"},  // sorting: user enables via UI
            {"partial_allocation", "true"},   // feature flag: always on
            {"role_combos",        "true"},   // feature flag: always on
        };
        for (int i = 0; i < defaults.length; i++) {
            AllocationRule rule = new AllocationRule();
            rule.setBusiness(business);
            rule.setRuleKey(defaults[i][0]);
            rule.setEnabled(Boolean.parseBoolean(defaults[i][1]));
            rule.setPriority(i + 1);
            allocationRuleRepository.save(rule);
        }

        return allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId);
    }

    private AllocationRuleDto toDto(AllocationRule rule) {
        return new AllocationRuleDto(
            rule.getRuleKey(),
            RULE_LABELS.getOrDefault(rule.getRuleKey(), rule.getRuleKey()),
            rule.isEnabled(),
            rule.getPriority()
        );
    }
}
