package com.populace.allocationrule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AllocationRulesUpdateRequest(
    @NotEmpty @Valid List<RuleEntry> rules
) {
    public record RuleEntry(
        @NotNull String ruleKey,
        @NotNull Boolean enabled,
        @NotNull Integer priority
    ) {}
}
