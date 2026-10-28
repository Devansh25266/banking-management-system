package com.bank.service;

import com.bank.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class BankUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public BankUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
