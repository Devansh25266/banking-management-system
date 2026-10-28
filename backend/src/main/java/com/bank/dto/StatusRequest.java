package com.bank.dto;

import com.bank.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "Status must be ACTIVE, FROZEN or CLOSED") AccountStatus status) {}
