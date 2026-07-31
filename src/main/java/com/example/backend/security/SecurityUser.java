package com.example.backend.security;

import com.example.backend.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUser implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final User.Role role;
    private final Long profileId;

    public SecurityUser(Long userId, String email, String password, User.Role role, Long profileId) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileId = profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public User.Role getRole() {
        return role;
    }

    public Long getProfileId() {
        return profileId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
