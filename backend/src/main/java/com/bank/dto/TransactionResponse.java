package com.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(long id, String referenceNumber, String type, BigDecimal amount,
    BigDecimal balanceBefore, BigDecimal balanceAfter, String status, String description,
    LocalDateTime createdAt, String accountNumber) {}
