package com.populace.domain;

import com.populace.domain.enums.ProficiencyLevel;
import com.populace.domain.enums.ShiftStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
public class Shift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "start_instant")
    private Instant startInstant;

    @Column(name = "end_instant")
    private Instant endInstant;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "break_duration_minutes")
    private Integer breakDurationMinutes = 0;

    // Calculated column in DB, read-only here
    @Column(name = "total_hours", insertable = false, updatable = false)
    private BigDecimal totalHours;

    @Column(name = "staff_required")
    private Integer staffRequired = 1;

    @Column(name = "staff_allocated")
    private Integer staffAllocated = 0;

    @Column(name = "fill_rate")
    private BigDecimal fillRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ShiftStatus status = ShiftStatus.open;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "required_proficiency_level")
    private ProficiencyLevel requiredProficiencyLevel = ProficiencyLevel.trainee;

    @Column(name = "template_shift_id")
    private Long templateShiftId;

    private String notes;

    @Version
    private Long version;

    public Shift() {
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

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(Integer breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public Integer getStaffRequired() {
        return staffRequired;
    }

    public void setStaffRequired(Integer staffRequired) {
        this.staffRequired = staffRequired;
    }

    public Integer getStaffAllocated() {
        return staffAllocated;
    }

    public void setStaffAllocated(Integer staffAllocated) {
        this.staffAllocated = staffAllocated;
    }

    public BigDecimal getFillRate() {
        return fillRate;
    }

    public void setFillRate(BigDecimal fillRate) {
        this.fillRate = fillRate;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }

    public Long getTemplateShiftId() {
        return templateShiftId;
    }

    public void setTemplateShiftId(Long templateShiftId) {
        this.templateShiftId = templateShiftId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getStartInstant() {
        return startInstant;
    }

    public void setStartInstant(Instant startInstant) {
        this.startInstant = startInstant;
    }

    public Instant getEndInstant() {
        return endInstant;
    }

    public void setEndInstant(Instant endInstant) {
        this.endInstant = endInstant;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ProficiencyLevel getRequiredProficiencyLevel() {
        return requiredProficiencyLevel;
    }

    public void setRequiredProficiencyLevel(ProficiencyLevel requiredProficiencyLevel) {
        this.requiredProficiencyLevel = requiredProficiencyLevel;
    }

    public Long getVersion() {
        return version;
    }

    public int getRemainingSlots() {
        return Math.max(0, staffRequired - staffAllocated);
    }

    public boolean isFilled() {
        return staffAllocated >= staffRequired;
    }

    public boolean isOpen() {
        return status == ShiftStatus.open || status == ShiftStatus.partially_filled;
    }
}
