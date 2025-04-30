// src/main/java/com/invoiceapp/security/AppUserDetails.java
package com.invoiceapp.security;

import com.invoiceapp.entity.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


//custom implementation of userdetails
public record AppUserDetails(User user) implements UserDetails {

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }
    @Override public String getPassword() {
        return user.getPasswordHash();
    }

    @Override public String getUsername() {
        return user.getEmail();
    }

    @Override public boolean isAccountNonExpired()  {
        return true;
    }
    @Override public boolean isAccountNonLocked()   {
        return true;
    }
    @Override public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override public boolean isEnabled() {
        return user.isEnabled();
    }
}
