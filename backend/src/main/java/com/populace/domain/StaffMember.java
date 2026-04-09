package com.populace.domain;

import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.EmploymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "staff_members")
public class StaffMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;

    private String phone;

    @Column(name = "secondary_phone")
    private String secondaryPhone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType = EmploymentType.permanent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "employment_status", nullable = false)
    private EmploymentStatus employmentStatus = EmploymentStatus.active;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    private String notes;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Mandatory Leave Enforcement Fields
    @Column(name = "must_go_on_leave_after_days")
    private Integer mustGoOnLeaveAfterDays;

    @Column(name = "accrues_one_day_leave_after_days")
    private Integer accruesOneDayLeaveAfterDays;

    @Column(name = "last_worked_date")
    private LocalDate lastWorkedDate;

    @Column(name = "consecutive_work_days")
    private Integer consecutiveWorkDays = 0;

    @Column(name = "last_accrual_reset_date")
    private LocalDate lastAccrualResetDate;

    @Column(name = "days_since_last_accrual")
    private Integer daysSinceLastAccrual = 0;

    public StaffMember() {
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
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

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
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

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public boolean isAvailableForAllocation() {
        return employmentStatus == EmploymentStatus.active && !isDeleted();
    }

    // Mandatory Leave Enforcement getters and setters

    public Integer getMustGoOnLeaveAfterDays() {
        return mustGoOnLeaveAfterDays;
    }

    public void setMustGoOnLeaveAfterDays(Integer mustGoOnLeaveAfterDays) {
        this.mustGoOnLeaveAfterDays = mustGoOnLeaveAfterDays;
    }

    public Integer getAccruesOneDayLeaveAfterDays() {
        return accruesOneDayLeaveAfterDays;
    }

    public void setAccruesOneDayLeaveAfterDays(Integer accruesOneDayLeaveAfterDays) {
        this.accruesOneDayLeaveAfterDays = accruesOneDayLeaveAfterDays;
    }

    public LocalDate getLastWorkedDate() {
        return lastWorkedDate;
    }

    public void setLastWorkedDate(LocalDate lastWorkedDate) {
        this.lastWorkedDate = lastWorkedDate;
    }

    public Integer getConsecutiveWorkDays() {
        return consecutiveWorkDays;
    }

    public void setConsecutiveWorkDays(Integer consecutiveWorkDays) {
        this.consecutiveWorkDays = consecutiveWorkDays;
    }

    public LocalDate getLastAccrualResetDate() {
        return lastAccrualResetDate;
    }

    public void setLastAccrualResetDate(LocalDate lastAccrualResetDate) {
        this.lastAccrualResetDate = lastAccrualResetDate;
    }

    public Integer getDaysSinceLastAccrual() {
        return daysSinceLastAccrual;
    }

    public void setDaysSinceLastAccrual(Integer daysSinceLastAccrual) {
        this.daysSinceLastAccrual = daysSinceLastAccrual;
    }

    
    /**
     * Returns the number of mandatory leave days accrued.
     * Staff accrues 1 day leave for every N consecutive work days.
     */
    public int getAccruedMandatoryLeaveDays() {
        if (accruesOneDayLeaveAfterDays == null || accruesOneDayLeaveAfterDays <= 0) {
            return 0;
        }
        if (consecutiveWorkDays == null) {
            return 0;
        }
        return consecutiveWorkDays / accruesOneDayLeaveAfterDays;
    }

    /**
     * Returns true if staff must go on mandatory leave.
     * This happens when consecutiveWorkDays reaches mustGoOnLeaveAfterDays.
     */
    public boolean mustTakeMandatoryLeave() {
        if (mustGoOnLeaveAfterDays == null || mustGoOnLeaveAfterDays <= 0) {
            return false;
        }
        return consecutiveWorkDays != null && consecutiveWorkDays >= mustGoOnLeaveAfterDays;
    }
}
