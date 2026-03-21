package com.finadvise.crm.users;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;


public record CustomUserDetails(@NonNull User user) implements UserDetails {

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    @NonNull
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getEmployeeId();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

}
