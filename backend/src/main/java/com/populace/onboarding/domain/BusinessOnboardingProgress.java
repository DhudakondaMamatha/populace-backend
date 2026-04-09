package com.populace.onboarding.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business_onboarding_progress")
public class BusinessOnboardingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "validation_result")
    private String validationResult;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public BusinessOnboardingProgress() {
    }

    public static BusinessOnboardingProgress createPending(Long businessId, Long stageId) {
        BusinessOnboardingProgress progress = new BusinessOnboardingProgress();
        progress.businessId = businessId;
        progress.stageId = stageId;
        progress.status = "pending";
        return progress;
    }

    public void markCompleted() {
        this.status = "completed";
        this.completedAt = Instant.now();
    }

    public void markPending() {
        this.status = "pending";
        this.completedAt = null;
    }

    public boolean isCompleted() {
        return "completed".equals(this.status);
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
