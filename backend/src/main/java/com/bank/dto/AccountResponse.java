package com.bank.dto;

import com.bank.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(long id, String accountNumber, String accountType, BigDecimal balance,
                              String currency, String status, LocalDateTime createdAt) {
    public static AccountResponse from(AccountRepository.Full a) {
        return new AccountResponse(a.id(), a.accountNumber(), a.accountType(), a.balance(), a.currency(), a.status(), a.createdAt());
    }
}
