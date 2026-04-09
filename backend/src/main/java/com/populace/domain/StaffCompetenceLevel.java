package com.populace.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Tracks competence levels (L1, L2, L3) achieved by staff members.
 * A staff member can hold multiple levels simultaneously.
 */
@Entity
@Table(name = "staff_competence_levels")
public class StaffCompetenceLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @Column(name = "level", nullable = false, length = 10)
    private String level;

    @Column(name = "achieved_at", nullable = false)
    private LocalDate achievedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public StaffCompetenceLevel() {
        this.createdAt = Instant.now();
        this.achievedAt = LocalDate.now();
    }

    public StaffCompetenceLevel(StaffMember staff, String level) {
        this();
        this.staff = staff;
        this.level = level;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocalDate getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(LocalDate achievedAt) {
        this.achievedAt = achievedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
