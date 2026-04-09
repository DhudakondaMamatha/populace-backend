package com.populace.auth.service;

import com.populace.domain.User;
import com.populace.domain.enums.PermissionLevel;
import com.populace.domain.enums.UserType;
import com.populace.platform.domain.PlatformAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final Long businessId;
    private final String email;
    private final String password;
    private final UserType userType;
    private final PermissionLevel permissionLevel;
    private final boolean active;
    private final boolean platformAdmin;
    private final boolean impersonating;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, Long businessId, String email, String password,
                          UserType userType, PermissionLevel permissionLevel,
                          boolean active, boolean platformAdmin, boolean impersonating,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.businessId = businessId;
        this.email = email;
        this.password = password;
        this.userType = userType;
        this.permissionLevel = permissionLevel;
        this.active = active;
        this.platformAdmin = platformAdmin;
        this.impersonating = impersonating;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        String roleName = user.getUserType().name().toUpperCase();
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + roleName));

        return new UserPrincipal(
            user.getId(),
            user.getBusinessId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getUserType(),
            user.getPermissionLevel(),
            user.isActive(),
            false,
            false,
            authorities);
    }

    public static UserPrincipal fromPlatformAdmin(PlatformAdmin admin) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

        return new UserPrincipal(
            admin.getId(),
            null,
            admin.getEmail(),
            admin.getPasswordHash(),
            null,
            null,
            admin.isActive(),
            true,
            false,
            authorities);
    }

    public static UserPrincipal forImpersonation(PlatformAdmin admin, Long businessId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));

        return new UserPrincipal(
            admin.getId(),
            businessId,
            admin.getEmail(),
            admin.getPasswordHash(),
            UserType.admin,
            PermissionLevel.ADMIN,
            admin.isActive(),
            true,
            true,
            authorities);
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public UserType getUserType() {
        return userType;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public boolean isImpersonating() {
        return impersonating;
    }

    public boolean isAdmin() {
        return permissionLevel == PermissionLevel.ADMIN;
    }

    public boolean isManagerOrAbove() {
        return permissionLevel == PermissionLevel.ADMIN
            || permissionLevel == PermissionLevel.MANAGER;
    }

    public boolean isExecutorOrAbove() {
        return permissionLevel == PermissionLevel.ADMIN
            || permissionLevel == PermissionLevel.MANAGER
            || permissionLevel == PermissionLevel.EXECUTOR;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
