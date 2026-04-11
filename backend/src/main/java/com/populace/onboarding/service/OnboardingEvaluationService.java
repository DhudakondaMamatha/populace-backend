package com.populace.onboarding.service;

import com.populace.onboarding.domain.BusinessOnboarding;
import com.populace.onboarding.domain.BusinessOnboardingProgress;
import com.populace.onboarding.domain.OnboardingStage;
import com.populace.onboarding.dto.OnboardingStatusResponse;
import com.populace.onboarding.dto.StageStatusDto;
import com.populace.onboarding.dto.StageValidationResult;
import com.populace.onboarding.repository.BusinessOnboardingProgressRepository;
import com.populace.onboarding.repository.BusinessOnboardingRepository;
import com.populace.onboarding.repository.OnboardingStageRepository;
import com.populace.onboarding.validator.StageValidator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OnboardingEvaluationService {

    private final OnboardingStageRepository stageRepository;
    private final BusinessOnboardingRepository onboardingRepository;
    private final BusinessOnboardingProgressRepository progressRepository;
    private final ApplicationContext applicationContext;

    public OnboardingEvaluationService(
            OnboardingStageRepository stageRepository,
            BusinessOnboardingRepository onboardingRepository,
            BusinessOnboardingProgressRepository progressRepository,
            ApplicationContext applicationContext) {
        this.stageRepository = stageRepository;
        this.onboardingRepository = onboardingRepository;
        this.progressRepository = progressRepository;
        this.applicationContext = applicationContext;
    }

    @Transactional
    public OnboardingStatusResponse evaluateAllStages(Long businessId) {
        List<OnboardingStage> allStages = loadAllStages();
        List<StageStatusDto> stageStatuses = new ArrayList<>();

        int completedStageCount = 0;
        String currentIncompleteStageCode = null;

        for (OnboardingStage stage : allStages) {
            StageValidationResult result = evaluateSingleStage(businessId, stage);
            updateProgressRecord(businessId, stage, result);

            if (result.complete()) {
                completedStageCount++;
            } else if (currentIncompleteStageCode == null) {
                currentIncompleteStageCode = stage.getCode();
            }

            stageStatuses.add(buildStageStatus(stage, result));
        }

        int totalStages = allStages.size();
        boolean isComplete = completedStageCount == totalStages;

        updateSummaryRecord(businessId, isComplete, currentIncompleteStageCode,
                           completedStageCount, totalStages);

        return buildResponse(isComplete, currentIncompleteStageCode,
                            completedStageCount, totalStages, stageStatuses);
    }

    public boolean isOnboardingComplete(Long businessId) {
        return onboardingRepository.existsByBusinessIdAndIsCompleteTrue(businessId);
    }

    private List<OnboardingStage> loadAllStages() {
        return stageRepository.findAllByOrderByDisplayOrderAsc();
    }

    private StageValidationResult evaluateSingleStage(Long businessId, OnboardingStage stage) {
        StageValidator validator = getValidatorBean(stage.getValidatorBean());
        return validator.validate(businessId);
    }

    private StageValidator getValidatorBean(String beanName) {
        return applicationContext.getBean(beanName, StageValidator.class);
    }

    private void updateProgressRecord(Long businessId, OnboardingStage stage,
                                       StageValidationResult result) {
        BusinessOnboardingProgress progress = findOrCreateProgress(businessId, stage.getId());

        if (result.complete()) {
            progress.markCompleted();
        } else {
            progress.markPending();
        }

        progressRepository.save(progress);
    }

    private BusinessOnboardingProgress findOrCreateProgress(Long businessId, Long stageId) {
        return progressRepository.findByBusinessIdAndStageId(businessId, stageId)
            .orElseGet(() -> BusinessOnboardingProgress.createPending(businessId, stageId));
    }

    private void updateSummaryRecord(Long businessId, boolean isComplete,
                                      String currentStageCode,
                                      int completedCount, int totalCount) {
        BusinessOnboarding summary = findOrCreateSummary(businessId);

        summary.setIsComplete(isComplete);
        summary.setCurrentStageCode(currentStageCode);
        summary.setStagesCompleted(completedCount);
        summary.setStagesTotal(totalCount);
        summary.setLastValidatedAt(Instant.now());

        onboardingRepository.save(summary);
    }

    private BusinessOnboarding findOrCreateSummary(Long businessId) {
        return onboardingRepository.findByBusinessId(businessId)
            .orElseGet(() -> BusinessOnboarding.createForBusiness(businessId));
    }

    private StageStatusDto buildStageStatus(OnboardingStage stage, StageValidationResult result) {
        return StageStatusDto.from(
            stage.getCode(),
            stage.getName(),
            stage.getDisplayOrder(),
            result.complete(),
            result.message()
        );
    }

    private OnboardingStatusResponse buildResponse(boolean isComplete,
                                                    String currentStageCode,
                                                    int completedCount,
                                                    int totalCount,
                                                    List<StageStatusDto> stages) {
        if (isComplete) {
            return OnboardingStatusResponse.complete(stages);
        }

        return OnboardingStatusResponse.incomplete(
            currentStageCode,
            completedCount,
            totalCount,
            stages
        );
    }
}
