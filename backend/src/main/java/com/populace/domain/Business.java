package com.populace.domain;

import com.populace.domain.enums.SubscriptionStatus;
import com.populace.domain.enums.SubscriptionTier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "businesses")
public class Business extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String address;

    private String industry;

    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "subscription_tier", nullable = false)
    private SubscriptionTier subscriptionTier = SubscriptionTier.starter;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.trial;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(name = "business_code", unique = true, length = 10)
    private String businessCode;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Scheduling Engine Fields
    @Column(name = "coverage_window_minutes")
    private Integer coverageWindowMinutes = 30;

    @Column(name = "tolerance_over_percentage", precision = 4, scale = 2)
    private BigDecimal toleranceOverPercentage = new BigDecimal("3.00");

    @Column(name = "tolerance_under_percentage", precision = 4, scale = 2)
    private BigDecimal toleranceUnderPercentage = new BigDecimal("10.00");

    // System-level constraint fields (legal/operational caps)
    @Column(name = "max_daily_hours_cap", precision = 4, scale = 2, nullable = false)
    private BigDecimal maxDailyHoursCap = new BigDecimal("12.00");

    @Column(name = "max_weekly_hours_cap", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxWeeklyHoursCap = new BigDecimal("48.00");

    @Column(name = "min_rest_between_shifts_hours", nullable = false)
    private Integer minRestBetweenShiftsHours = 11;

    @Column(name = "max_consecutive_days", nullable = false)
    private Integer maxConsecutiveDays = 6;

    @Column(name = "min_break_duration_minutes", nullable = false)
    private Integer minBreakDurationMinutes = 30;

    @Column(name = "max_continuous_work_minutes", nullable = false)
    private Integer maxContinuousWorkMinutes = 300;

    @Column(name = "min_work_before_break_minutes", nullable = false)
    private Integer minWorkBeforeBreakMinutes = 180;

    @Column(name = "min_days_at_site_before_move", nullable = false)
    private Integer minDaysAtSiteBeforeMove = 1;

    @Column(name = "default_max_breaks_per_shift", nullable = false)
    private Integer defaultMaxBreaksPerShift = 2;

    public Business() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SubscriptionTier getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(SubscriptionTier subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Integer getCoverageWindowMinutes() {
        return coverageWindowMinutes;
    }

    public void setCoverageWindowMinutes(Integer coverageWindowMinutes) {
        this.coverageWindowMinutes = coverageWindowMinutes;
    }

    public BigDecimal getToleranceOverPercentage() {
        return toleranceOverPercentage;
    }

    public void setToleranceOverPercentage(BigDecimal toleranceOverPercentage) {
        this.toleranceOverPercentage = toleranceOverPercentage;
    }

    public BigDecimal getToleranceUnderPercentage() {
        return toleranceUnderPercentage;
    }

    public void setToleranceUnderPercentage(BigDecimal toleranceUnderPercentage) {
        this.toleranceUnderPercentage = toleranceUnderPercentage;
    }

    // System constraint getters and setters

    public BigDecimal getMaxDailyHoursCap() {
        return maxDailyHoursCap;
    }

    public void setMaxDailyHoursCap(BigDecimal maxDailyHoursCap) {
        this.maxDailyHoursCap = maxDailyHoursCap;
    }

    public BigDecimal getMaxWeeklyHoursCap() {
        return maxWeeklyHoursCap;
    }

    public void setMaxWeeklyHoursCap(BigDecimal maxWeeklyHoursCap) {
        this.maxWeeklyHoursCap = maxWeeklyHoursCap;
    }

    public Integer getMinRestBetweenShiftsHours() {
        return minRestBetweenShiftsHours;
    }

    public void setMinRestBetweenShiftsHours(Integer minRestBetweenShiftsHours) {
        this.minRestBetweenShiftsHours = minRestBetweenShiftsHours;
    }

    public Integer getMaxConsecutiveDays() {
        return maxConsecutiveDays;
    }

    public void setMaxConsecutiveDays(Integer maxConsecutiveDays) {
        this.maxConsecutiveDays = maxConsecutiveDays;
    }

    public Integer getMinBreakDurationMinutes() {
        return minBreakDurationMinutes;
    }

    public void setMinBreakDurationMinutes(Integer minBreakDurationMinutes) {
        this.minBreakDurationMinutes = minBreakDurationMinutes;
    }

    public Integer getMaxContinuousWorkMinutes() {
        return maxContinuousWorkMinutes;
    }

    public void setMaxContinuousWorkMinutes(Integer maxContinuousWorkMinutes) {
        this.maxContinuousWorkMinutes = maxContinuousWorkMinutes;
    }

    public Integer getMinWorkBeforeBreakMinutes() {
        return minWorkBeforeBreakMinutes;
    }

    public void setMinWorkBeforeBreakMinutes(Integer minWorkBeforeBreakMinutes) {
        this.minWorkBeforeBreakMinutes = minWorkBeforeBreakMinutes;
    }

    public Integer getMinDaysAtSiteBeforeMove() {
        return minDaysAtSiteBeforeMove;
    }

    public void setMinDaysAtSiteBeforeMove(Integer minDaysAtSiteBeforeMove) {
        this.minDaysAtSiteBeforeMove = minDaysAtSiteBeforeMove;
    }

    public Integer getDefaultMaxBreaksPerShift() {
        return defaultMaxBreaksPerShift;
    }

    public void setDefaultMaxBreaksPerShift(Integer defaultMaxBreaksPerShift) {
        this.defaultMaxBreaksPerShift = defaultMaxBreaksPerShift;
    }
}
