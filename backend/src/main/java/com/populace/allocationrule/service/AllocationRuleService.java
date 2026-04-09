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

@Service
public class AllocationRuleService {

    private static final Map<String, String> RULE_LABELS = Map.of(
        "cheapest",        "Catch by cheapest in position",
        "expertise",       "Catch by expertise level in position",
        "hours_available", "Catch by most hours available"
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

        String[] keys = {"cheapest", "expertise", "hours_available"};
        for (int i = 0; i < keys.length; i++) {
            AllocationRule rule = new AllocationRule();
            rule.setBusiness(business);
            rule.setRuleKey(keys[i]);
            rule.setEnabled(i == 0); // only cheapest enabled by default
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
