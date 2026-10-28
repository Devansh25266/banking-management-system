package com.bank.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name is too long") String fullName,
    @NotBlank(message = "Email is required") @Email(message = "Email is invalid") @Size(max = 150) String email,
    @NotBlank(message = "Phone is required") @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be 10-15 digits") String phone,
    @NotBlank(message = "Address is required") @Size(max = 255, message = "Address is too long") String address,
    @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain a letter and a digit") String password,
    @NotBlank(message = "Please confirm your password") String confirmPassword) {}
