package com.populace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "shift_roles")
public class ShiftRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "staff_required", nullable = false)
    private Integer staffRequired = 1;

    @Column(name = "staff_allocated", nullable = false)
    private Integer staffAllocated = 0;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

    public ShiftRole() {
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Long getShiftId() {
        return shift != null ? shift.getId() : null;
    }

    @JsonIgnore
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }

    public String getRoleName() {
        return role != null ? role.getName() : null;
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

    public int getRemainingSlots() {
        return Math.max(0, staffRequired - staffAllocated);
    }

    public boolean isFilled() {
        return staffAllocated >= staffRequired;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
