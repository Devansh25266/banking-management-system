package com.bank.service;

import com.bank.dto.*;
import com.bank.exception.BankException;
import com.bank.model.AccountStatus;
import com.bank.repository.AccountRepository;
import com.bank.repository.AdminRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bank.model.AccountType;
import java.security.SecureRandom;
import org.springframework.dao.DuplicateKeyException;

@Service
public class AdminService {
    private final AccountRepository accounts;
    private final AdminRepository admin;
    private final AuditRepository audit;
    private final UserRepository users;

    public AdminService(AccountRepository accounts, AdminRepository admin, AuditRepository audit, UserRepository users) {
        this.accounts = accounts; this.admin = admin; this.audit = audit; this.users = users;
    }

    private static final SecureRandom ACCOUNT_RNG = new SecureRandom();

    @Transactional
    public AccountResponse createAccount(long adminId, long customerId, AccountType type) {
        if (type == null) throw new BankException(HttpStatus.BAD_REQUEST, "Choose SAVINGS or CURRENT account type.");
        if (!users.isActiveCustomer(customerId))
            throw new BankException(HttpStatus.BAD_REQUEST, "Select an active customer before opening an account.");
        for (int attempt = 0; attempt < 5; attempt++) {
            String number = String.format("10%010d", ACCOUNT_RNG.nextLong(10_000_000_000L));
            try {
                accounts.insert(number, customerId, type.name());
                var created = accounts.findByNumber(number).orElseThrow();
                audit.insert(adminId, "ACCOUNT_CREATED", "ACCOUNT", created.id());
                return AccountResponse.from(created);
            } catch (DuplicateKeyException collision) {
                // Generate another account number if a rare collision occurs.
            }
        }
        throw new BankException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create account. Please try again.");
    }

    public DashboardResponse dashboard(int days) {
        if (days < 1 || days > 365) throw new BankException(HttpStatus.BAD_REQUEST, "Days must be between 1 and 365.");
        return admin.dashboard(days);
    }

    public PageResponse<AdminAccountResponse> accounts(String search, AccountStatus status, int page, int size) {
        checkPaging(page, size);
        return admin.searchAccounts(search, status, page, Math.min(size, TransactionQueryService.MAX_PAGE_SIZE));
    }

    public PageResponse<AdminCustomerResponse> customers(String search, String status, int page, int size) {
        checkPaging(page, size);
        if (status != null && !status.isBlank() && !status.equals("ACTIVE") && !status.equals("DISABLED"))
            throw new BankException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or DISABLED.");
        return admin.searchCustomers(search, status, page, Math.min(size, TransactionQueryService.MAX_PAGE_SIZE));
    }

    @Transactional
    public void recordCustomerCreated(long adminId, long customerId) {
        audit.insert(adminId, "CUSTOMER_CREATED", "USER", customerId);
    }

    @Transactional
    public void updateCustomerStatus(long adminId, long customerId, String status) {
        if (!status.equals("ACTIVE") && !status.equals("DISABLED"))
            throw new BankException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or DISABLED.");
        if (users.updateCustomerStatus(customerId, status) == 0)
            throw new BankException(HttpStatus.NOT_FOUND, "Customer not found.");
        audit.insert(adminId, "CUSTOMER_STATUS_" + status, "USER", customerId);
    }

    public PageResponse<AuditLogResponse> auditLogs(int page, int size) {
        checkPaging(page, size);
        return audit.list(page, Math.min(size, TransactionQueryService.MAX_PAGE_SIZE));
    }

    /** The status change and its audit record commit together or not at all. */
    @Transactional
    public AccountResponse updateStatus(long adminId, long accountId, AccountStatus newStatus) {
        var acc = accounts.findByIdForUpdate(accountId)
            .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Account not found."));
        if ("CLOSED".equals(acc.status()))
            throw new BankException(HttpStatus.CONFLICT, "Closed accounts cannot be changed.");
        if (acc.status().equals(newStatus.name()))
            throw new BankException(HttpStatus.CONFLICT, "Account is already " + newStatus.name().toLowerCase() + ".");
        if (newStatus == AccountStatus.CLOSED && acc.balance().signum() != 0)
            throw new BankException(HttpStatus.CONFLICT, "Balance must be zero before an account can be closed.");
        accounts.updateStatus(accountId, newStatus.name());
        audit.insert(adminId, "ACCOUNT_STATUS_" + newStatus.name(), "ACCOUNT", accountId);
        return AccountResponse.from(accounts.findById(accountId).orElseThrow());
    }

    private void checkPaging(int page, int size) {
        if (page < 0 || size < 1) throw new BankException(HttpStatus.BAD_REQUEST, "Page must be 0 or more and size at least 1.");
    }
}
