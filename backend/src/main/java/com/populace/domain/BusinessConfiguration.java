package com.populace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Business-level configuration settings.
 * Stores operational parameters like monthly hour tolerance.
 */
@Entity
@Table(name = "business_configuration")
public class BusinessConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false, unique = true)
    private Business business;

    @Column(name = "monthly_hour_tolerance_percent", nullable = false)
    private BigDecimal monthlyHourTolerancePercent = new BigDecimal("10.00");

    public BusinessConfiguration() {
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Long getBusinessId() {
        return business != null ? business.getId() : null;
    }

    public BigDecimal getMonthlyHourTolerancePercent() {
        return monthlyHourTolerancePercent;
    }

    public void setMonthlyHourTolerancePercent(BigDecimal monthlyHourTolerancePercent) {
        this.monthlyHourTolerancePercent = monthlyHourTolerancePercent;
    }
}
