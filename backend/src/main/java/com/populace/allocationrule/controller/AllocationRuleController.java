package com.populace.allocationrule.controller;

import com.populace.allocationrule.dto.AllocationRuleDto;
import com.populace.allocationrule.dto.AllocationRulesUpdateRequest;
import com.populace.allocationrule.service.AllocationRuleService;
import com.populace.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business/allocation-rules")
public class AllocationRuleController {

    private final AllocationRuleService allocationRuleService;

    public AllocationRuleController(AllocationRuleService allocationRuleService) {
        this.allocationRuleService = allocationRuleService;
    }

    @GetMapping
    public ResponseEntity<List<AllocationRuleDto>> getRules(
            @AuthenticationPrincipal UserPrincipal user) {
        List<AllocationRuleDto> rules = allocationRuleService.getRules(user.getBusinessId());
        return ResponseEntity.ok(rules);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<List<AllocationRuleDto>> updateRules(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AllocationRulesUpdateRequest request) {
        List<AllocationRuleDto> rules = allocationRuleService.updateRules(user.getBusinessId(), request);
        return ResponseEntity.ok(rules);
    }
}
