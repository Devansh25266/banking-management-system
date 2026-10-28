package com.bank.service;

import com.bank.dto.RegisterRequest;
import com.bank.dto.UserResponse;
import com.bank.exception.BankException;
import com.bank.model.BankUser;
import com.bank.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    public UserResponse register(RegisterRequest r) {
        if (!r.password().equals(r.confirmPassword()))
            throw new BankException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new BankException(HttpStatus.CONFLICT, "An account with this email already exists.");
        String hash = encoder.encode(r.password());
        long id;
        try {
            id = users.insert(r.fullName().trim(), email, hash, r.phone().trim(), r.address().trim());
        } catch (DuplicateKeyException e) {   // concurrent registration with the same email
            throw new BankException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        return UserResponse.from(new BankUser(id, r.fullName().trim(), email, hash, r.phone().trim(), r.address().trim(), "CUSTOMER", true));
    }
}
