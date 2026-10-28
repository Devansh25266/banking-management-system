package com.bank.controller;

import com.bank.dto.*;
import com.bank.model.AccountStatus;
import com.bank.model.BankUser;
import com.bank.model.TransactionType;
import com.bank.service.AdminService;
import com.bank.service.AuthService;
import com.bank.service.TransactionQueryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** Access is restricted to ROLE_ADMIN by SecurityConfig for every /api/admin/** path. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final TransactionQueryService transactions;
    private final AuthService auth;

    public AdminController(AdminService admin, TransactionQueryService transactions, AuthService auth) {
        this.admin = admin; this.transactions = transactions; this.auth = auth;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "30") int days) { return admin.dashboard(days); }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AdminCreateAccountRequest r, @AuthenticationPrincipal BankUser u) {
        return admin.createAccount(u.getId(), r.customerId(), r.accountType());
    }

    @GetMapping("/accounts")
    public PageResponse<AdminAccountResponse> accounts(@RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return admin.accounts(search, status, page, size);
    }

    @GetMapping("/customers")
    public PageResponse<AdminCustomerResponse> customers(@RequestParam(required = false) String search,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return admin.customers(search, status, page, size);
    }

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createCustomer(@Valid @RequestBody RegisterRequest r, @AuthenticationPrincipal BankUser u) {
        UserResponse created = auth.register(r);
        // Record the creation after the user has been successfully created.
        admin.recordCustomerCreated(u.getId(), created.id());
        return created;
    }

    @PatchMapping("/customers/{customerId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void customerStatus(@PathVariable long customerId, @Valid @RequestBody UserStatusRequest r, @AuthenticationPrincipal BankUser u) {
        admin.updateCustomerStatus(u.getId(), customerId, r.status());
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AccountResponse status(@PathVariable long accountId, @Valid @RequestBody StatusRequest r, @AuthenticationPrincipal BankUser u) {
        return admin.updateStatus(u.getId(), accountId, r.status());
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> transactions(@RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        return transactions.search(accountId, from, to, type, page, size, sort);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> audit(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return admin.auditLogs(page, size);
    }
}
