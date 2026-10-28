package com.bank.service;

import com.bank.exception.BankException;
import com.bank.model.AccountType;
import com.bank.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AccountRepository repo;

    AccountRepository.Full row(String number, long userId) {
        return new AccountRepository.Full(1, number, userId, "SAVINGS", new BigDecimal("0.00"), "INR", "ACTIVE", LocalDateTime.now());
    }

    @Test void createsZeroBalanceInrAccountWithTenPrefixedNumber() {
        when(repo.findByNumber(anyString())).thenAnswer(i -> Optional.of(row(i.getArgument(0), 5)));
        var res = new AccountService(repo).create(5, AccountType.SAVINGS);
        verify(repo).insert(matches("10\\d{10}"), eq(5L), eq("SAVINGS"));
        assertEquals(0, res.balance().compareTo(BigDecimal.ZERO));
        assertEquals("INR", res.currency());
    }
    @Test void retriesOnAccountNumberCollision() {
        doThrow(new DuplicateKeyException("dup")).doNothing().when(repo).insert(anyString(), anyLong(), anyString());
        when(repo.findByNumber(anyString())).thenAnswer(i -> Optional.of(row(i.getArgument(0), 5)));
        new AccountService(repo).create(5, AccountType.CURRENT);
        verify(repo, times(2)).insert(anyString(), eq(5L), eq("CURRENT"));
    }
    @Test void otherCustomersAccountIsNotFound() {
        when(repo.findByIdAndUser(9, 5)).thenReturn(Optional.empty());
        var ex = assertThrows(BankException.class, () -> new AccountService(repo).get(5, 9));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
