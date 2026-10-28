package com.bank.controller;

import com.bank.dto.*;
import com.bank.exception.BankException;
import com.bank.model.BankUser;
import com.bank.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthenticationManager manager;
    private final HttpSessionSecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService auth, AuthenticationManager manager) { this.auth = auth; this.manager = manager; }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
        Authentication result;
        try {
            result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(r.email().trim().toLowerCase(), r.password()));
        } catch (AuthenticationException e) {
            throw new BankException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        if (req.getSession(false) != null) req.changeSessionId();   // prevent session fixation
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(result);
        SecurityContextHolder.setContext(ctx);
        contextRepo.saveContext(ctx, req, res);
        return UserResponse.from((BankUser) result.getPrincipal());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal BankUser user) { return UserResponse.from(user); }
}
