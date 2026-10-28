package com.bank.dto;

import com.bank.model.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminCreateAccountRequest(
    @Positive(message = "Choose a customer") long customerId,
    @NotNull(message = "Account type must be SAVINGS or CURRENT") AccountType accountType
) {}
