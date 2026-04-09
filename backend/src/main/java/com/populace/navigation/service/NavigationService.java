package com.populace.navigation.service;

import com.populace.navigation.dto.NavItemDto;
import com.populace.onboarding.service.OnboardingEvaluationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NavigationService {

    private final OnboardingEvaluationService onboardingService;

    private static final List<NavItemConfig> ALL_NAV_ITEMS = List.of(
        new NavItemConfig("/dashboard", "Dashboard", 1, true),
        new NavItemConfig("/sites", "Sites", 2, false),
        new NavItemConfig("/roles", "Roles", 3, false),
        new NavItemConfig("/staff", "Staff", 4, false),
        new NavItemConfig("/shifts", "Shifts", 5, false),
        new NavItemConfig("/schedule", "Schedule", 6, true),
        new NavItemConfig("/allocation", "Allocation", 7, true),
        //new NavItemConfig("/allocation-runs", "Allocation Runs", 8, true),
        new NavItemConfig("/leave", "Leave", 8, true),
        new NavItemConfig("/reports", "Reports", 9, true),
        new NavItemConfig("/users", "Users", 10, true),
        new NavItemConfig("/settings", "Settings", 11, true)
    );

    public NavigationService(OnboardingEvaluationService onboardingService) {
        this.onboardingService = onboardingService;
    }

    public List<NavItemDto> getNavigationForBusiness(Long businessId) {
        boolean onboardingComplete = onboardingService.isOnboardingComplete(businessId);

        List<NavItemDto> visibleItems = new ArrayList<>();

        for (NavItemConfig config : ALL_NAV_ITEMS) {
            if (shouldShowItem(config, onboardingComplete)) {
                visibleItems.add(config.toDto());
            }
        }

        visibleItems.sort(Comparator.comparingInt(NavItemDto::displayOrder));

        return visibleItems;
    }

    private boolean shouldShowItem(NavItemConfig config, boolean onboardingComplete) {
        if (!config.requiresOnboarding()) {
            return true;
        }
        return onboardingComplete;
    }

    private record NavItemConfig(
        String path,
        String label,
        int displayOrder,
        boolean requiresOnboarding
    ) {
        NavItemDto toDto() {
            return NavItemDto.of(path, label, displayOrder);
        }
    }
}
