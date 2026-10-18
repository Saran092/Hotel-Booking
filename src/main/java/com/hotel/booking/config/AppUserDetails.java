package com.hotel.booking.config;

import com.hotel.booking.model.Role;
import com.hotel.booking.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/** Spring Security view of a {@link User}. Deactivated and locked accounts are rejected by the framework. */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String displayName;
    private final Role primaryRole;
    private final boolean enabled;
    private final boolean nonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.displayName = user.getFullName();
        this.primaryRole = user.getPrimaryRole();
        this.enabled = user.isActive();
        this.nonLocked = !user.isLocked();
        this.authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Role getPrimaryRole() { return primaryRole; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return nonLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
