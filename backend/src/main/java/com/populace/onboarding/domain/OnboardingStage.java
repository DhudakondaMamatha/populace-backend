package com.populace.onboarding.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "onboarding_stages")
public class OnboardingStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "validator_bean", nullable = false, length = 100)
    private String validatorBean;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public OnboardingStage() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getValidatorBean() {
        return validatorBean;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
