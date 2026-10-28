package com.bank.service;

import com.bank.exception.BankException;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountRepository.Row;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock AccountRepository accounts;
    @Mock TransactionRepository transactions;

    TransactionService service() { return new TransactionService(accounts, transactions); }
    void account(String balance, String status) {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(new Row(1, 10, new BigDecimal(balance), status)));
    }

    @Test void depositAddsAmountAndRecordsTransaction() {
        account("100.00", "ACTIVE");
        BigDecimal after = service().deposit(10, 1, new BigDecimal("50.25"), "salary");
        assertEquals(new BigDecimal("150.25"), after);
        verify(accounts).updateBalance(1, new BigDecimal("150.25"));
        verify(transactions).insert(startsWith("TXN"), eq(1L), eq("DEPOSIT"), eq(new BigDecimal("50.25")),
            eq(new BigDecimal("100.00")), eq(new BigDecimal("150.25")), eq("salary"));
    }
    @Test void withdrawSubtractsAmountAndRecordsTransaction() {
        account("100.00", "ACTIVE");
        BigDecimal after = service().withdraw(10, 1, new BigDecimal("40.10"), null);
        assertEquals(new BigDecimal("59.90"), after);
        verify(transactions).insert(anyString(), eq(1L), eq("WITHDRAWAL"), eq(new BigDecimal("40.10")),
            eq(new BigDecimal("100.00")), eq(new BigDecimal("59.90")), isNull());
    }
    @Test void withdrawingExactBalanceLeavesZero() {
        account("25.00", "ACTIVE");
        assertEquals(0, service().withdraw(10, 1, new BigDecimal("25.00"), null).compareTo(BigDecimal.ZERO));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "0.00", "-5", "0.001", "10.555"})
    void zeroNegativeAndTooPreciseAmountsRejected(String amount) {
        var ex1 = assertThrows(BankException.class, () -> service().deposit(10, 1, new BigDecimal(amount), null));
        var ex2 = assertThrows(BankException.class, () -> service().withdraw(10, 1, new BigDecimal(amount), null));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
        verifyNoInteractions(accounts, transactions);
    }
    @Test void insufficientBalanceChangesNothing() {
        account("20.00", "ACTIVE");
        var ex = assertThrows(BankException.class, () -> service().withdraw(10, 1, new BigDecimal("20.01"), null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(accounts, never()).updateBalance(anyLong(), any());
        verifyNoInteractions(transactions);
    }
    @ParameterizedTest @ValueSource(strings = {"FROZEN", "CLOSED"})
    void frozenAndClosedAccountsRejectBothOperations(String status) {
        account("100.00", status);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BankException.class, () -> service().deposit(10, 1, BigDecimal.ONE, null)).getStatus());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BankException.class, () -> service().withdraw(10, 1, BigDecimal.ONE, null)).getStatus());
        verify(accounts, never()).updateBalance(anyLong(), any());
        verifyNoInteractions(transactions);
    }
    @Test void anotherCustomersAccountLooksNonExistent() {
        account("100.00", "ACTIVE");
        var ex = assertThrows(BankException.class, () -> service().withdraw(999, 1, BigDecimal.TEN, null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(accounts, never()).updateBalance(anyLong(), any());
    }
}
