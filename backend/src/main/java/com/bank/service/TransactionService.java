package com.bank.service;

import com.bank.exception.BankException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TransactionService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransactionService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Transactional
    public BigDecimal deposit(long userId, long accountId, BigDecimal amount, String description) {
        return apply(userId, accountId, amount, description, true);
    }

    @Transactional
    public BigDecimal withdraw(long userId, long accountId, BigDecimal amount, String description) {
        return apply(userId, accountId, amount, description, false);
    }

    private BigDecimal apply(long userId, long accountId, BigDecimal amount, String description, boolean deposit) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new BankException(HttpStatus.BAD_REQUEST, "Amount must be positive with at most 2 decimals.");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);

        var acc = accounts.findByIdForUpdate(accountId)
            .filter(a -> a.userId() == userId)   // hides other customers' accounts
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Account not found."));
        if (!"ACTIVE".equals(acc.status()))
            throw new BankException(HttpStatus.CONFLICT, "Account is " + acc.status().toLowerCase() + ".");

        BigDecimal before = acc.balance();
        if (!deposit && before.compareTo(amount) < 0)
            throw new BankException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance.");
        BigDecimal after = deposit ? before.add(amount) : before.subtract(amount);

        accounts.updateBalance(accountId, after);
        transactions.insert("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(),
            accountId, deposit ? "DEPOSIT" : "WITHDRAWAL", amount, before, after, description);
        return after;
    }
}
