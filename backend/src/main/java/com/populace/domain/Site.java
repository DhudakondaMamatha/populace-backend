package com.populace.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sites")
public class Site extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    private String code;

    private String address;

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

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Scheduling Engine Fields
    @Column(name = "max_concurrent_break_percentage", precision = 3, scale = 2)
    private BigDecimal maxConcurrentBreakPercentage = new BigDecimal("0.25");

    @Column(name = "min_coverage_during_break")
    private Integer minCoverageDuringBreak = 1;

    @Column(name = "enforce_same_role_break_exclusivity")
    private boolean enforceSameRoleBreakExclusivity = true;

    public Site() {
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public BigDecimal getMaxConcurrentBreakPercentage() {
        return maxConcurrentBreakPercentage;
    }

    public void setMaxConcurrentBreakPercentage(BigDecimal maxConcurrentBreakPercentage) {
        this.maxConcurrentBreakPercentage = maxConcurrentBreakPercentage;
    }

    public Integer getMinCoverageDuringBreak() {
        return minCoverageDuringBreak;
    }

    public void setMinCoverageDuringBreak(Integer minCoverageDuringBreak) {
        this.minCoverageDuringBreak = minCoverageDuringBreak;
    }

    public boolean isEnforceSameRoleBreakExclusivity() {
        return enforceSameRoleBreakExclusivity;
    }

    public void setEnforceSameRoleBreakExclusivity(boolean enforceSameRoleBreakExclusivity) {
        this.enforceSameRoleBreakExclusivity = enforceSameRoleBreakExclusivity;
    }

}
