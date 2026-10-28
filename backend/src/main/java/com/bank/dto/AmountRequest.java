package com.bank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AmountRequest(
    @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax("10000000.00") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @Size(max = 255) String description) {}
