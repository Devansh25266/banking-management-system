package com.bank.dto;

import com.bank.model.BankUser;

public record UserResponse(long id, String fullName, String email, String phone, String address, String role) {
    public static UserResponse from(BankUser u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAddress(), u.getRole());
    }
}
