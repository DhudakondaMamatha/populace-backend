package com.populace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Business-wide work parameter defaults.
 * Defines labor regulations and scheduling constraints.
 */
@Entity
@Table(name = "work_parameters")
public class WorkParameters extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(name = "min_hours_per_day")
    private BigDecimal minHoursPerDay;

    @Column(name = "max_hours_per_day")
    private BigDecimal maxHoursPerDay;

    @Column(name = "min_hours_per_week")
    private BigDecimal minHoursPerWeek;

    @Column(name = "max_hours_per_week")
    private BigDecimal maxHoursPerWeek;

    @Column(name = "min_days_off_per_week")
    private Integer minDaysOffPerWeek;

    @Column(name = "min_hours_per_month")
    private BigDecimal minHoursPerMonth;

    @Column(name = "max_hours_per_month")
    private BigDecimal maxHoursPerMonth;

    @Column(name = "break_after_hours")
    private BigDecimal breakAfterHours;

    @Column(name = "min_break_duration_minutes")
    private Integer minBreakDurationMinutes;

    @Column(name = "min_rest_between_shifts_hours")
    private BigDecimal minRestBetweenShiftsHours;

    @Column(name = "daily_overtime_threshold")
    private BigDecimal dailyOvertimeThreshold;

    @Column(name = "weekly_overtime_threshold")
    private BigDecimal weeklyOvertimeThreshold;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "is_active")
    private boolean active = true;

    public WorkParameters() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getBreakAfterHours() {
        return breakAfterHours;
    }

    public void setBreakAfterHours(BigDecimal breakAfterHours) {
        this.breakAfterHours = breakAfterHours;
    }

    public Integer getMinBreakDurationMinutes() {
        return minBreakDurationMinutes;
    }

    public void setMinBreakDurationMinutes(Integer minBreakDurationMinutes) {
        this.minBreakDurationMinutes = minBreakDurationMinutes;
    }

    public BigDecimal getMinRestBetweenShiftsHours() {
        return minRestBetweenShiftsHours;
    }

    public void setMinRestBetweenShiftsHours(BigDecimal minRestBetweenShiftsHours) {
        this.minRestBetweenShiftsHours = minRestBetweenShiftsHours;
    }

    public BigDecimal getDailyOvertimeThreshold() {
        return dailyOvertimeThreshold;
    }

    public void setDailyOvertimeThreshold(BigDecimal dailyOvertimeThreshold) {
        this.dailyOvertimeThreshold = dailyOvertimeThreshold;
    }

    public BigDecimal getWeeklyOvertimeThreshold() {
        return weeklyOvertimeThreshold;
    }

    public void setWeeklyOvertimeThreshold(BigDecimal weeklyOvertimeThreshold) {
        this.weeklyOvertimeThreshold = weeklyOvertimeThreshold;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
