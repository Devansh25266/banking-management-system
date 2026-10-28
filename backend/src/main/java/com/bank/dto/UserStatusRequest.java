package com.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED", message = "Status must be ACTIVE or DISABLED") String status) {}
