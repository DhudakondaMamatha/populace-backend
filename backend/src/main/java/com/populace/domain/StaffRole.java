package com.populace.domain;

import com.populace.domain.enums.ProficiencyLevel;
import com.populace.domain.enums.SkillLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

@Entity
@Table(name = "staff_roles")
public class StaffRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Unified skill level for this staff-role assignment.
     * Used in allocation ranking (L3 > L2 > L1) and eligibility checks.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "skill_level", length = 2)
    private SkillLevel skillLevel = SkillLevel.L2;

    // TODO: Remove proficiencyLevel after migration verification
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "proficiency_level")
    @Deprecated
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.competent;

    @Column(name = "is_primary")
    private boolean primary = false;

    @Column(name = "certified_at")
    private LocalDate certifiedAt;

    @Column(name = "certification_expires_at")
    private LocalDate certificationExpiresAt;

    // Break Configuration Override (inherits from role, can override)
    @Column(name = "min_break_minutes")
    private Integer minBreakMinutes;

    @Column(name = "max_break_minutes")
    private Integer maxBreakMinutes;

    @Column(name = "max_break_duration_minutes")
    private Integer maxBreakDurationMinutes;

    @Column(name = "min_work_minutes_before_break")
    private Integer minWorkMinutesBeforeBreak;

    @Column(name = "max_continuous_work_minutes")
    private Integer maxContinuousWorkMinutes;

    @Column(name = "is_active")
    private boolean active = true;

    // TODO: Remove competenceLevel after migration verification
    @Column(name = "competence_level", length = 2)
    @Deprecated
    private String competenceLevel;

    public StaffRole() {
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    /**
     * @deprecated Use getSkillLevel() instead
     */
    @Deprecated
    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    /**
     * @deprecated Use setSkillLevel() instead
     */
    @Deprecated
    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public LocalDate getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(LocalDate certifiedAt) {
        this.certifiedAt = certifiedAt;
    }

    public LocalDate getCertificationExpiresAt() {
        return certificationExpiresAt;
    }

    public void setCertificationExpiresAt(LocalDate certificationExpiresAt) {
        this.certificationExpiresAt = certificationExpiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCertificationValid() {
        if (certificationExpiresAt == null) {
            return true;
        }
        return !LocalDate.now().isAfter(certificationExpiresAt);
    }

    public Integer getMinBreakMinutes() {
        return minBreakMinutes;
    }

    public void setMinBreakMinutes(Integer minBreakMinutes) {
        this.minBreakMinutes = minBreakMinutes;
    }

    public Integer getMaxBreakMinutes() {
        return maxBreakMinutes;
    }

    public void setMaxBreakMinutes(Integer maxBreakMinutes) {
        this.maxBreakMinutes = maxBreakMinutes;
    }

    /**
     * Get effective min break minutes.
     * Post-refactor: Break config is now on Business (system) level, not Role.
     */
    public Integer getEffectiveMinBreakMinutes() {
        return minBreakMinutes;
    }

    /**
     * Get effective max break minutes.
     * Post-refactor: Break config is now on Business (system) level, not Role.
     */
    public Integer getEffectiveMaxBreakMinutes() {
        return maxBreakMinutes;
    }

    public Integer getMaxBreakDurationMinutes() {
        return maxBreakDurationMinutes;
    }

    public void setMaxBreakDurationMinutes(Integer maxBreakDurationMinutes) {
        this.maxBreakDurationMinutes = maxBreakDurationMinutes;
    }

    /**
     * Get effective max break duration minutes.
     * Post-refactor: Break config is now on Business (system) level, not Role.
     */
    public Integer getEffectiveMaxBreakDurationMinutes() {
        return maxBreakDurationMinutes;
    }

    public Integer getMinWorkMinutesBeforeBreak() {
        return minWorkMinutesBeforeBreak;
    }

    public void setMinWorkMinutesBeforeBreak(Integer minWorkMinutesBeforeBreak) {
        this.minWorkMinutesBeforeBreak = minWorkMinutesBeforeBreak;
    }

    public Integer getMaxContinuousWorkMinutes() {
        return maxContinuousWorkMinutes;
    }

    public void setMaxContinuousWorkMinutes(Integer maxContinuousWorkMinutes) {
        this.maxContinuousWorkMinutes = maxContinuousWorkMinutes;
    }

    public Integer getEffectiveMinWorkMinutesBeforeBreak() {
        return minWorkMinutesBeforeBreak;
    }

    public Integer getEffectiveMaxContinuousWorkMinutes() {
        return maxContinuousWorkMinutes;
    }

    /**
     * Check if this StaffRole has any break override values.
     */
    public boolean hasBreakOverride() {
        return minBreakMinutes != null || maxBreakMinutes != null
            || maxBreakDurationMinutes != null
            || minWorkMinutesBeforeBreak != null
            || maxContinuousWorkMinutes != null;
    }

    public String getCompetenceLevel() {
        return competenceLevel;
    }

    public void setCompetenceLevel(String competenceLevel) {
        this.competenceLevel = competenceLevel;
    }
}
