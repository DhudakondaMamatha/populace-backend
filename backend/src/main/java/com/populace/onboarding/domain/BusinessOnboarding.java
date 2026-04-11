package com.populace.onboarding.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business_onboarding")
public class BusinessOnboarding {

    @Id
    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete = false;

    @Column(name = "current_stage_code", length = 50)
    private String currentStageCode;

    @Column(name = "stages_completed", nullable = false)
    private Integer stagesCompleted = 0;

    @Column(name = "stages_total", nullable = false)
    private Integer stagesTotal = 0;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public BusinessOnboarding() {
    }

    public static BusinessOnboarding createForBusiness(Long businessId) {
        BusinessOnboarding onboarding = new BusinessOnboarding();
        onboarding.businessId = businessId;
        onboarding.isComplete = false;
        onboarding.stagesCompleted = 0;
        onboarding.stagesTotal = 0;
        return onboarding;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public String getCurrentStageCode() {
        return currentStageCode;
    }

    public void setCurrentStageCode(String currentStageCode) {
        this.currentStageCode = currentStageCode;
    }

    public Integer getStagesCompleted() {
        return stagesCompleted;
    }

    public void setStagesCompleted(Integer stagesCompleted) {
        this.stagesCompleted = stagesCompleted;
    }

    public Integer getStagesTotal() {
        return stagesTotal;
    }

    public void setStagesTotal(Integer stagesTotal) {
        this.stagesTotal = stagesTotal;
    }

    public Instant getLastValidatedAt() {
        return lastValidatedAt;
    }

    public void setLastValidatedAt(Instant lastValidatedAt) {
        this.lastValidatedAt = lastValidatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
