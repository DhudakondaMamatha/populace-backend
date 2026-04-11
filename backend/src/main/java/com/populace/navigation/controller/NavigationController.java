package com.populace.navigation.controller;

import com.populace.auth.util.SecurityUtils;
import com.populace.navigation.dto.NavItemDto;
import com.populace.navigation.service.NavigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;

    public NavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping
    public ResponseEntity<List<NavItemDto>> getNavigation() {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        List<NavItemDto> items = navigationService.getNavigationForBusiness(businessId);
        return ResponseEntity.ok(items);
    }
}
