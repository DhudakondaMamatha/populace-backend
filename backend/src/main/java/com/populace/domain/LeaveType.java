package com.populace.domain;

import com.populace.domain.enums.AccrualPeriod;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_types")
public class LeaveType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String code;

    @Column(name = "is_paid")
    private boolean paid = true;

    @Column(name = "accrual_rate", precision = 6, scale = 4)
    private BigDecimal accrualRate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "accrual_period")
    private AccrualPeriod accrualPeriod = AccrualPeriod.monthly;

    @Column(name = "max_balance", precision = 6, scale = 2)
    private BigDecimal maxBalance;

    @Column(name = "carry_over_limit", precision = 6, scale = 2)
    private BigDecimal carryOverLimit;

    @Column(name = "requires_approval")
    private boolean requiresApproval = true;

    @Column(name = "min_notice_days")
    private Integer minNoticeDays = 0;

    @Column(length = 7)
    private String color;

    @Column(name = "is_active")
    private boolean active = true;

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public BigDecimal getAccrualRate() { return accrualRate; }
    public void setAccrualRate(BigDecimal rate) { this.accrualRate = rate; }
    public AccrualPeriod getAccrualPeriod() { return accrualPeriod; }
    public void setAccrualPeriod(AccrualPeriod period) { this.accrualPeriod = period; }
    public BigDecimal getMaxBalance() { return maxBalance; }
    public void setMaxBalance(BigDecimal maxBalance) { this.maxBalance = maxBalance; }
    public BigDecimal getCarryOverLimit() { return carryOverLimit; }
    public void setCarryOverLimit(BigDecimal limit) { this.carryOverLimit = limit; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean req) { this.requiresApproval = req; }
    public Integer getMinNoticeDays() { return minNoticeDays; }
    public void setMinNoticeDays(Integer days) { this.minNoticeDays = days; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
