package com.bank.service;

import com.bank.dto.PageResponse;
import com.bank.exception.BankException;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {
    @Mock AccountRepository accounts;
    @Mock TransactionRepository transactions;

    TransactionQueryService service() { return new TransactionQueryService(accounts, transactions); }
    void owned() {
        when(accounts.findByIdAndUser(1, 5)).thenReturn(Optional.of(new AccountRepository.Full(1, "1000000001", 5, "SAVINGS",
            BigDecimal.ZERO, "INR", "ACTIVE", LocalDateTime.now())));
        lenient().when(transactions.search(any(), anyInt(), anyInt())).thenReturn(PageResponse.of(List.of(), 0, 20, 0));
    }

    @Test void otherCustomersHistoryIsNotFound() {
        when(accounts.findByIdAndUser(1, 6)).thenReturn(Optional.empty());
        var ex = assertThrows(BankException.class, () -> service().history(6, 1, null, null, null, 0, 20, "desc"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(transactions);
    }
    @Test void dateRangeBecomesInclusiveDayBounds() {
        owned();
        service().history(5, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30), TransactionType.DEPOSIT, 0, 20, "asc");
        var c = ArgumentCaptor.forClass(TransactionRepository.Filter.class);
        verify(transactions).search(c.capture(), eq(0), eq(20));
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), c.getValue().from());
        assertEquals(LocalDateTime.of(2026, 1, 31, 0, 0), c.getValue().toExclusive());
        assertEquals(TransactionType.DEPOSIT, c.getValue().type());
        assertTrue(c.getValue().ascending());
    }
    @Test void pageSizeIsCappedAt100() {
        owned();
        service().history(5, 1, null, null, null, 0, 5000, "desc");
        verify(transactions).search(any(), eq(0), eq(100));
    }
    @Test void invalidParametersRejected() {
        owned();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BankException.class,
            () -> service().history(5, 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, 0, 20, "desc")).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BankException.class,
            () -> service().history(5, 1, null, null, null, -1, 20, "desc")).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BankException.class,
            () -> service().history(5, 1, null, null, null, 0, 20, "amount; DROP TABLE users")).getStatus());
    }
}
