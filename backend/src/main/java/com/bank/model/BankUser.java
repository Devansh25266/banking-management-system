package com.bank.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class BankUser implements UserDetails, Serializable {
    private final long id;
    private final String fullName, email, passwordHash, phone, address, role;
    private final boolean active;

    public BankUser(long id, String fullName, String email, String passwordHash,
                    String phone, String address, String role, boolean active) {
        this.id = id; this.fullName = fullName; this.email = email; this.passwordHash = passwordHash;
        this.phone = phone; this.address = address; this.role = role; this.active = active;
    }
    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getRole() { return role; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role)); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return active; }
}
