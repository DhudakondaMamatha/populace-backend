package com.populace.onboarding.dto;

import java.util.List;

public record OnboardingStatusResponse(
    boolean isComplete,
    String currentStageCode,
    int stagesCompleted,
    int stagesTotal,
    List<StageStatusDto> stages
) {
    public static OnboardingStatusResponse complete(List<StageStatusDto> stages) {
        return new OnboardingStatusResponse(
            true,
            null,
            stages.size(),
            stages.size(),
            stages
        );
    }

    public static OnboardingStatusResponse incomplete(
            String currentStageCode,
            int completed,
            int total,
            List<StageStatusDto> stages) {
        return new OnboardingStatusResponse(
            false,
            currentStageCode,
            completed,
            total,
            stages
        );
    }
}
