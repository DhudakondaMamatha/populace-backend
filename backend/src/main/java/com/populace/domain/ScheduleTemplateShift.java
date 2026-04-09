package com.populace.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_template_shifts")
public class ScheduleTemplateShift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScheduleTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_combo_id")
    private RoleCombo roleCombo;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "break_duration_minutes")
    private Integer breakDurationMinutes = 0;

    @Column(name = "staff_required")
    private Integer staffRequired = 1;

    private String notes;

    public ScheduleTemplateShift() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ScheduleTemplate getTemplate() { return template; }
    public void setTemplate(ScheduleTemplate template) { this.template = template; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public RoleCombo getRoleCombo() { return roleCombo; }
    public void setRoleCombo(RoleCombo roleCombo) { this.roleCombo = roleCombo; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getBreakDurationMinutes() { return breakDurationMinutes; }
    public void setBreakDurationMinutes(Integer breakDurationMinutes) { this.breakDurationMinutes = breakDurationMinutes; }

    public Integer getStaffRequired() { return staffRequired; }
    public void setStaffRequired(Integer staffRequired) { this.staffRequired = staffRequired; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
