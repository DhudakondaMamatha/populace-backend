package com.populace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Staff-specific work parameter overrides.
 * When set, these override the business-wide defaults for a specific staff member.
 */
@Entity
@Table(name = "staff_work_parameters")
public class StaffWorkParameters extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_parameter_id")
    private WorkParameters workParameter;

    @Column(name = "min_hours_per_day", nullable = false)
    private BigDecimal minHoursPerDay;

    @Column(name = "max_hours_per_day", nullable = false)
    private BigDecimal maxHoursPerDay;

    @Column(name = "min_hours_per_week", nullable = false)
    private BigDecimal minHoursPerWeek;

    @Column(name = "max_hours_per_week", nullable = false)
    private BigDecimal maxHoursPerWeek;

    @Column(name = "min_days_off_per_week", nullable = false)
    private Integer minDaysOffPerWeek;

    @Column(name = "min_hours_per_month", nullable = false)
    private BigDecimal minHoursPerMonth;

    @Column(name = "max_hours_per_month", nullable = false)
    private BigDecimal maxHoursPerMonth;

    @Column(name = "max_sites_per_day", nullable = false)
    private Integer maxSitesPerDay;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public StaffWorkParameters() {
    }

    public Long getId() {
        return id;
    }

    public StaffMember getStaff() {
        return staff;
    }

    public void setStaff(StaffMember staff) {
        this.staff = staff;
    }

    public WorkParameters getWorkParameter() {
        return workParameter;
    }

    public void setWorkParameter(WorkParameters workParameter) {
        this.workParameter = workParameter;
    }

    public BigDecimal getMinHoursPerDay() {
        return minHoursPerDay;
    }

    public void setMinHoursPerDay(BigDecimal minHoursPerDay) {
        this.minHoursPerDay = minHoursPerDay;
    }

    public BigDecimal getMaxHoursPerDay() {
        return maxHoursPerDay;
    }

    public void setMaxHoursPerDay(BigDecimal maxHoursPerDay) {
        this.maxHoursPerDay = maxHoursPerDay;
    }

    public BigDecimal getMinHoursPerWeek() {
        return minHoursPerWeek;
    }

    public void setMinHoursPerWeek(BigDecimal minHoursPerWeek) {
        this.minHoursPerWeek = minHoursPerWeek;
    }

    public BigDecimal getMaxHoursPerWeek() {
        return maxHoursPerWeek;
    }

    public void setMaxHoursPerWeek(BigDecimal maxHoursPerWeek) {
        this.maxHoursPerWeek = maxHoursPerWeek;
    }

    public Integer getMinDaysOffPerWeek() {
        return minDaysOffPerWeek;
    }

    public void setMinDaysOffPerWeek(Integer minDaysOffPerWeek) {
        this.minDaysOffPerWeek = minDaysOffPerWeek;
    }

    public BigDecimal getMinHoursPerMonth() {
        return minHoursPerMonth;
    }

    public void setMinHoursPerMonth(BigDecimal minHoursPerMonth) {
        this.minHoursPerMonth = minHoursPerMonth;
    }

    public BigDecimal getMaxHoursPerMonth() {
        return maxHoursPerMonth;
    }

    public void setMaxHoursPerMonth(BigDecimal maxHoursPerMonth) {
        this.maxHoursPerMonth = maxHoursPerMonth;
    }

    public Integer getMaxSitesPerDay() {
        return maxSitesPerDay;
    }

    public void setMaxSitesPerDay(Integer maxSitesPerDay) {
        this.maxSitesPerDay = maxSitesPerDay;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    /**
     * Check if these parameters are currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        boolean afterStart = !today.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !today.isAfter(effectiveTo);
        return afterStart && beforeEnd;
    }
}
