package com.bank.service;

import com.bank.repository.UserRepository;
import com.bank.exception.BankException;
import com.bank.model.AccountStatus;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountRepository.Row;
import com.bank.repository.AdminRepository;
import com.bank.repository.AuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock AccountRepository accounts;
    @Mock AdminRepository adminRepo;
    @Mock AuditRepository audit;
    @Mock UserRepository users;

    AdminService service() {

        return new AdminService(accounts, adminRepo, audit, users);
    }
    void existing(String status, String balance) {
        when(accounts.findByIdForUpdate(5)).thenReturn(Optional.of(new Row(5, 2, new BigDecimal(balance), status)));
    }

    @Test void freezingWritesStatusAndAuditEntry() {
        existing("ACTIVE", "10.00");
        when(accounts.findById(5)).thenReturn(Optional.of(new AccountRepository.Full(5, "1000000005", 2, "SAVINGS",
            new BigDecimal("10.00"), "INR", "FROZEN", LocalDateTime.now())));
        var res = service().updateStatus(1, 5, AccountStatus.FROZEN);
        assertEquals("FROZEN", res.status());
        verify(accounts).updateStatus(5, "FROZEN");
        verify(audit).insert(1, "ACCOUNT_STATUS_FROZEN", "ACCOUNT", 5);
    }
    @Test void closedAccountCannotBeReopened() {
        existing("CLOSED", "0.00");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BankException.class, () -> service().updateStatus(1, 5, AccountStatus.ACTIVE)).getStatus());
        verify(accounts, never()).updateStatus(anyLong(), anyString());
        verifyNoInteractions(audit);
    }
    @Test void accountWithBalanceCannotBeClosed() {
        existing("ACTIVE", "0.01");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BankException.class, () -> service().updateStatus(1, 5, AccountStatus.CLOSED)).getStatus());
        verifyNoInteractions(audit);
    }
    @Test void sameStatusIsRejectedWithoutAuditNoise() {
        existing("FROZEN", "0.00");
        assertThrows(BankException.class, () -> service().updateStatus(1, 5, AccountStatus.FROZEN));
        verifyNoInteractions(audit);
    }
    @Test void unknownAccountIs404() {
        when(accounts.findByIdForUpdate(77)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BankException.class, () -> service().updateStatus(1, 77, AccountStatus.FROZEN)).getStatus());
    }
    @Test void dashboardPeriodValidated() {
        assertThrows(BankException.class, () -> service().dashboard(0));
        assertThrows(BankException.class, () -> service().dashboard(366));
    }
}
