package com.bank.service;

import com.bank.dto.AccountResponse;
import com.bank.exception.BankException;
import com.bank.model.AccountType;
import com.bank.repository.AccountRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {
    private static final SecureRandom RNG = new SecureRandom();
    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) { this.accounts = accounts; }

    public AccountResponse create(long userId, AccountType type) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String number = String.format("10%010d", RNG.nextLong(10_000_000_000L));
            try {
                accounts.insert(number, userId, type.name());
                return AccountResponse.from(accounts.findByNumber(number).orElseThrow());
            } catch (DuplicateKeyException e) { /* number collision: generate another */ }
        }
        throw new BankException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create account. Please try again.");
    }
    public List<AccountResponse> list(long userId) {
        return accounts.findByUser(userId).stream().map(AccountResponse::from).toList();
    }
    /** Another customer's account is reported as not found so its existence is not revealed. */
    public AccountResponse get(long userId, long accountId) {
        return accounts.findByIdAndUser(accountId, userId).map(AccountResponse::from)
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    public com.bank.dto.BalanceResponse balance(long userId, long accountId) {
        var a = accounts.findByIdAndUser(accountId, userId)
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Account not found."));
        return new com.bank.dto.BalanceResponse(a.id(), a.accountNumber(), a.balance(), a.currency(), a.status());
    }
}
