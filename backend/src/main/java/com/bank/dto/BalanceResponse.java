package com.bank.dto;

import java.math.BigDecimal;

public record BalanceResponse(long accountId, String accountNumber, BigDecimal balance, String currency, String status) {}
