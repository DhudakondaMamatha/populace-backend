package com.populace.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "role_combo_roles")
public class RoleComboRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_combo_id", nullable = false)
    private RoleCombo roleCombo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public RoleComboRole() {}

    public Long getId() { return id; }

    public RoleCombo getRoleCombo() { return roleCombo; }
    public void setRoleCombo(RoleCombo roleCombo) { this.roleCombo = roleCombo; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
