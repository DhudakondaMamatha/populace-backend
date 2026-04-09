package com.populace.allocationrule.dto;

public record AllocationRuleDto(
    String ruleKey,
    String label,
    boolean enabled,
    int priority
) {}
