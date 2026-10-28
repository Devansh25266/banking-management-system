package com.bank.dto;

import java.time.LocalDateTime;

public record AdminCustomerResponse(long id, String fullName, String email, String phone, String address, String status, LocalDateTime createdAt) {}
