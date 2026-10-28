package com.bank.integration;

import com.bank.exception.BankException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

/** Runs against a real MySQL 8 container. Skipped automatically when Docker is not available. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FinancialIntegrationTest {
    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired TransactionService service;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired JdbcTemplate jdbc;
    @SpyBean TransactionRepository transactions;

    record Acct(long userId, long accountId) {}

    Acct newAccount(String balance) {
        long userId = users.insert("Test User", "t" + UUID.randomUUID() + "@example.com", "hash", "9876543210", "addr");
        String number = "10" + String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
        accounts.insert(number, userId, "SAVINGS");
        long id = accounts.findByNumber(number).orElseThrow().id();
        jdbc.update("UPDATE accounts SET balance = ? WHERE id = ?", new BigDecimal(balance), id);   // test-only seeding
        return new Acct(userId, id);
    }
    BigDecimal balance(long id) { return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, id); }
    long txCount(long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE account_id = ?", Long.class, id); }

    /** Runs the tasks simultaneously and returns how many succeeded. */
    int race(int tasks, Callable<Object> op) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < tasks; i++) results.add(pool.submit(() -> {
            start.await();
            try { op.call(); return true; } catch (BankException e) { return false; }
        }));
        start.countDown();
        int ok = 0;
        for (var f : results) if (f.get(60, TimeUnit.SECONDS)) ok++;
        pool.shutdown();
        return ok;
    }

    @Test void concurrentWithdrawalsNeverOverdraw() throws Exception {
        Acct a = newAccount("100.00");
        AtomicInteger n = new AtomicInteger();
        int ok = race(20, () -> service.withdraw(a.userId(), a.accountId(), new BigDecimal("30.00"), "w" + n.incrementAndGet()));
        assertEquals(3, ok);
        assertEquals(0, new BigDecimal("10.00").compareTo(balance(a.accountId())));
        assertEquals(3, txCount(a.accountId()));
    }
    @Test void concurrentDepositsAreAllApplied() throws Exception {
        Acct a = newAccount("0.00");
        assertEquals(20, race(20, () -> service.deposit(a.userId(), a.accountId(), new BigDecimal("5.00"), null)));
        assertEquals(0, new BigDecimal("100.00").compareTo(balance(a.accountId())));
        assertEquals(20, txCount(a.accountId()));
    }
    @Test void failureWhileRecordingTransactionRollsBackBalance() {
        Acct a = newAccount("100.00");
        doThrow(new DataAccessResourceFailureException("simulated failure")).when(transactions)
            .insert(anyString(), anyLong(), anyString(), any(), any(), any(), any());
        assertThrows(RuntimeException.class, () -> service.withdraw(a.userId(), a.accountId(), new BigDecimal("40.00"), null));
        assertEquals(0, new BigDecimal("100.00").compareTo(balance(a.accountId())));
        assertEquals(0, txCount(a.accountId()));
    }
    @Test void rejectedOperationsLeaveNoTransactionRecord() {
        Acct a = newAccount("10.00");
        assertThrows(BankException.class, () -> service.withdraw(a.userId(), a.accountId(), new BigDecimal("10.01"), null));
        assertThrows(BankException.class, () -> service.deposit(a.userId() + 1000, a.accountId(), BigDecimal.ONE, null));
        assertEquals(0, txCount(a.accountId()));
        assertEquals(0, new BigDecimal("10.00").compareTo(balance(a.accountId())));
    }
    @Test void historyIsAContinuousLedgerMatchingTheBalance() {
        Acct a = newAccount("0.00");
        service.deposit(a.userId(), a.accountId(), new BigDecimal("100.00"), null);
        service.withdraw(a.userId(), a.accountId(), new BigDecimal("30.00"), null);
        service.deposit(a.userId(), a.accountId(), new BigDecimal("5.50"), null);
        service.withdraw(a.userId(), a.accountId(), new BigDecimal("10.00"), null);
        var rows = transactions.search(new TransactionRepository.Filter(a.accountId(), null, null, null, true), 0, 100).content();
        assertEquals(4, rows.size());
        BigDecimal running = BigDecimal.ZERO;
        for (var t : rows) {
            assertEquals(0, running.compareTo(t.balanceBefore()));
            running = "DEPOSIT".equals(t.type()) ? running.add(t.amount()) : running.subtract(t.amount());
            assertEquals(0, running.compareTo(t.balanceAfter()));
        }
        assertEquals(0, running.compareTo(balance(a.accountId())));
    }
}
