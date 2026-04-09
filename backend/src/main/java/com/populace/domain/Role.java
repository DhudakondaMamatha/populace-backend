package com.populace.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    private String code;

    private String description;

    @Column(length = 7)
    private String color;

    // Break Configuration (role-level, falls back to business if NULL)
    @Column(name = "break_required_after_hours", precision = 5, scale = 2)
    private BigDecimal breakRequiredAfterHours;

    @Column(name = "break_duration_minutes")
    private Integer breakDurationMinutes;

    @Column(name = "max_continuous_work_minutes")
    private Integer maxContinuousWorkMinutes;

    @Column(name = "max_breaks_per_shift")
    private Integer maxBreaksPerShift;

    @Column(name = "is_default", nullable = false)
    private boolean defaultRole = false;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Role() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(boolean defaultRole) {
        this.defaultRole = defaultRole;
    }

    public BigDecimal getBreakRequiredAfterHours() {
        return breakRequiredAfterHours;
    }

    public void setBreakRequiredAfterHours(BigDecimal breakRequiredAfterHours) {
        this.breakRequiredAfterHours = breakRequiredAfterHours;
    }

    public Integer getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(Integer breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public Integer getMaxContinuousWorkMinutes() {
        return maxContinuousWorkMinutes;
    }

    public void setMaxContinuousWorkMinutes(Integer maxContinuousWorkMinutes) {
        this.maxContinuousWorkMinutes = maxContinuousWorkMinutes;
    }

    public Integer getMaxBreaksPerShift() {
        return maxBreaksPerShift;
    }

    public void setMaxBreaksPerShift(Integer maxBreaksPerShift) {
        this.maxBreaksPerShift = maxBreaksPerShift;
    }

}
