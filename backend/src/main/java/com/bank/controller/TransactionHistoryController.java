package com.bank.controller;

import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.model.BankUser;
import com.bank.model.TransactionType;
import com.bank.service.TransactionQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class TransactionHistoryController {
    private final TransactionQueryService service;
    public TransactionHistoryController(TransactionQueryService service) { this.service = service; }

    @GetMapping("/accounts/{accountId}/transactions")
    public PageResponse<TransactionResponse> history(@PathVariable long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort, @AuthenticationPrincipal BankUser u) {
        return service.history(u.getId(), accountId, from, to, type, page, size, sort);
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionResponse one(@PathVariable long transactionId, @AuthenticationPrincipal BankUser u) {
        return service.get(u.getId(), transactionId);
    }
}
