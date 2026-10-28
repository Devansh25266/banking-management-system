package com.bank.service;

import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.exception.BankException;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class TransactionQueryService {
    public static final int MAX_PAGE_SIZE = 100;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransactionQueryService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public PageResponse<TransactionResponse> history(long userId, long accountId, LocalDate from, LocalDate to,
                                                     TransactionType type, int page, int size, String sort) {
        accounts.findByIdAndUser(accountId, userId)
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Account not found."));
        return search(accountId, from, to, type, page, size, sort);
    }

    /** Shared by customer history and admin monitoring; the caller is responsible for authorization. */
    public PageResponse<TransactionResponse> search(Long accountId, LocalDate from, LocalDate to,
                                                    TransactionType type, int page, int size, String sort) {
        if (page < 0 || size < 1) throw new BankException(HttpStatus.BAD_REQUEST, "Page must be 0 or more and size at least 1.");
        if (from != null && to != null && from.isAfter(to)) throw new BankException(HttpStatus.BAD_REQUEST, "'from' date must not be after 'to' date.");
        boolean ascending;
        if ("asc".equalsIgnoreCase(sort)) ascending = true;
        else if ("desc".equalsIgnoreCase(sort)) ascending = false;
        else throw new BankException(HttpStatus.BAD_REQUEST, "Sort must be 'asc' or 'desc'.");
        var filter = new TransactionRepository.Filter(accountId, from == null ? null : from.atStartOfDay(),
            to == null ? null : to.plusDays(1).atStartOfDay(), type, ascending);
        return transactions.search(filter, page, Math.min(size, MAX_PAGE_SIZE));
    }

    public TransactionResponse get(long userId, long transactionId) {
        return transactions.findByIdAndUser(transactionId, userId)
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Transaction not found."));
    }
}
