package com.populace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "staff_leave_balances")
public class StaffLeaveBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "opening_balance", precision = 6, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(precision = 6, scale = 2)
    private BigDecimal accrued = BigDecimal.ZERO;

    @Column(precision = 6, scale = 2)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(precision = 6, scale = 2)
    private BigDecimal adjusted = BigDecimal.ZERO;

    // Computed column in DB - read only
    @Column(name = "current_balance", insertable = false, updatable = false)
    private BigDecimal currentBalance;

    public Long getId() { return id; }
    public StaffMember getStaff() { return staff; }
    public void setStaff(StaffMember staff) { this.staff = staff; }
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType type) { this.leaveType = type; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal bal) { this.openingBalance = bal; }
    public BigDecimal getAccrued() { return accrued; }
    public void setAccrued(BigDecimal accrued) { this.accrued = accrued; }
    public BigDecimal getUsed() { return used; }
    public void setUsed(BigDecimal used) { this.used = used; }
    public BigDecimal getAdjusted() { return adjusted; }
    public void setAdjusted(BigDecimal adjusted) { this.adjusted = adjusted; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
}
