package com.bank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class AccountRepository {
    public record Row(long id, long userId, BigDecimal balance, String status) {}
    private final JdbcTemplate jdbc;
    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Locks the row until the surrounding transaction ends (SELECT ... FOR UPDATE). */
    public Optional<Row> findByIdForUpdate(long id) {
        return jdbc.query("SELECT id, user_id, balance, status FROM accounts WHERE id = ? FOR UPDATE",
            (rs, i) -> new Row(rs.getLong("id"), rs.getLong("user_id"), rs.getBigDecimal("balance"), rs.getString("status")),
            id).stream().findFirst();
    }
    public void updateBalance(long id, BigDecimal newBalance) {
        jdbc.update("UPDATE accounts SET balance = ? WHERE id = ?", newBalance, id);
    }

    public record Full(long id, String accountNumber, long userId, String accountType, BigDecimal balance,
                       String currency, String status, java.time.LocalDateTime createdAt) {}
    private static final org.springframework.jdbc.core.RowMapper<Full> FULL = (rs, i) -> new Full(rs.getLong("id"),
        rs.getString("account_number"), rs.getLong("user_id"), rs.getString("account_type"), rs.getBigDecimal("balance"),
        rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());

    /** Balance starts at 0.00 and currency at INR via column defaults; callers cannot set either. */
    public void insert(String accountNumber, long userId, String type) {
        jdbc.update("INSERT INTO accounts (account_number, user_id, account_type) VALUES (?,?,?)", accountNumber, userId, type);
    }
    public Optional<Full> findByNumber(String n) {
        return jdbc.query("SELECT * FROM accounts WHERE account_number = ?", FULL, n).stream().findFirst();
    }
    public java.util.List<Full> findByUser(long userId) {
        return jdbc.query("SELECT * FROM accounts WHERE user_id = ? ORDER BY created_at DESC, id DESC", FULL, userId);
    }
    public Optional<Full> findByIdAndUser(long id, long userId) {
        return jdbc.query("SELECT * FROM accounts WHERE id = ? AND user_id = ?", FULL, id, userId).stream().findFirst();
    }

    public Optional<Full> findById(long id) {
        return jdbc.query("SELECT * FROM accounts WHERE id = ?", FULL, id).stream().findFirst();
    }
    public void updateStatus(long id, String status) {
        jdbc.update("UPDATE accounts SET status = ? WHERE id = ?", status, id);
    }
}
