package com.bank.controller;

import com.bank.dto.*;
import com.bank.model.BankUser;
import com.bank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest r, @AuthenticationPrincipal BankUser u) {
        return service.create(u.getId(), r.accountType());
    }
    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal BankUser u) { return service.list(u.getId()); }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable long accountId, @AuthenticationPrincipal BankUser u) {
        return service.get(u.getId(), accountId);
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse balance(@PathVariable long accountId, @AuthenticationPrincipal BankUser u) {
        return service.balance(u.getId(), accountId);
    }
}
