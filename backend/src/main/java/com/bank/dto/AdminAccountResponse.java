package com.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminAccountResponse(long id, String accountNumber, String accountType, BigDecimal balance, String currency,
                                   String status, LocalDateTime createdAt, String ownerName, String ownerEmail) {}
