package com.bank.controller;

import com.bank.dto.AmountRequest;
import com.bank.service.TransactionService;
import jakarta.validation.Valid;
import com.bank.model.BankUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts/{accountId}")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping("/deposit")
    public Map<String, BigDecimal> deposit(@PathVariable long accountId, @Valid @RequestBody AmountRequest r, @AuthenticationPrincipal BankUser u) {
        return Map.of("balance", service.deposit(u.getId(), accountId, r.amount(), r.description()));
    }
    @PostMapping("/withdraw")
    public Map<String, BigDecimal> withdraw(@PathVariable long accountId, @Valid @RequestBody AmountRequest r, @AuthenticationPrincipal BankUser u) {
        return Map.of("balance", service.withdraw(u.getId(), accountId, r.amount(), r.description()));
    }
}
