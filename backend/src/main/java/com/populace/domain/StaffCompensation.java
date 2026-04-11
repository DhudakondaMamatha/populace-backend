package com.populace.domain;

import com.populace.domain.enums.CompensationType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compensation record for a staff member.
 * Supports both hourly and monthly compensation models.
 */
@Entity
@Table(name = "staff_compensation")
public class StaffCompensation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "compensation_type", nullable = false, length = 10)
    private CompensationType compensationType = CompensationType.hourly;

    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    public StaffCompensation() {
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

    public Long getStaffId() {
        return staff != null ? staff.getId() : null;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
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

    public CompensationType getCompensationType() {
        return compensationType;
    }

    public void setCompensationType(CompensationType compensationType) {
        this.compensationType = compensationType;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    /**
     * Checks if this compensation record is currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        boolean startedOrToday = !effectiveFrom.isAfter(today);
        boolean notEnded = effectiveTo == null || !effectiveTo.isBefore(today);
        return startedOrToday && notEnded;
    }

    /**
     * Checks if this is an hourly compensation record.
     */
    public boolean isHourly() {
        return compensationType == CompensationType.hourly;
    }

    /**
     * Checks if this is a monthly salary compensation record.
     */
    public boolean isMonthly() {
        return compensationType == CompensationType.monthly;
    }
}
