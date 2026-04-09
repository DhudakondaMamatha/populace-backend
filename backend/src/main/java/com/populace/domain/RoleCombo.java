package com.populace.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role_combos")
public class RoleCombo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "roleCombo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoleComboRole> roleComboRoles = new ArrayList<>();

    public RoleCombo() {}

    public Long getId() { return id; }

    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<RoleComboRole> getRoleComboRoles() { return roleComboRoles; }
    public void setRoleComboRoles(List<RoleComboRole> roleComboRoles) { this.roleComboRoles = roleComboRoles; }
}
