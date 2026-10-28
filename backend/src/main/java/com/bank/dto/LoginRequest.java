package com.bank.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank(message = "Email is required") String email,
                           @NotBlank(message = "Password is required") String password) {}
