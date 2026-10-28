package com.bank.repository;

import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.model.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {
    /** accountId null means all accounts (admin monitoring). toExclusive is an exclusive upper bound. */
    public record Filter(Long accountId, LocalDateTime from, LocalDateTime toExclusive, TransactionType type, boolean ascending) {}

    private static final String SELECT = "SELECT t.*, a.account_number FROM transactions t JOIN accounts a ON a.id = t.account_id";
    private static final RowMapper<TransactionResponse> MAPPER = (rs, i) -> new TransactionResponse(
        rs.getLong("id"), rs.getString("reference_number"), rs.getString("transaction_type"), rs.getBigDecimal("amount"),
        rs.getBigDecimal("balance_before"), rs.getBigDecimal("balance_after"), rs.getString("status"),
        rs.getString("description"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("account_number"));

    private final JdbcTemplate jdbc;
    public TransactionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(String ref, long accountId, String type, BigDecimal amount,
                       BigDecimal before, BigDecimal after, String description) {
        jdbc.update("INSERT INTO transactions (reference_number, account_id, transaction_type, amount, balance_before, balance_after, status, description) VALUES (?,?,?,?,?,?, 'SUCCESS', ?)",
            ref, accountId, type, amount, before, after, description);
    }

    public PageResponse<TransactionResponse> search(Filter f, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (f.accountId() != null) { where.append(" AND t.account_id = ?"); args.add(f.accountId()); }
        if (f.from() != null) { where.append(" AND t.created_at >= ?"); args.add(Timestamp.valueOf(f.from())); }
        if (f.toExclusive() != null) { where.append(" AND t.created_at < ?"); args.add(Timestamp.valueOf(f.toExclusive())); }
        if (f.type() != null) { where.append(" AND t.transaction_type = ?"); args.add(f.type().name()); }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM transactions t" + where, Long.class, args.toArray());
        String dir = f.ascending() ? "ASC" : "DESC";   // constant strings only; never user text
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<TransactionResponse> rows = jdbc.query(SELECT + where + " ORDER BY t.created_at " + dir + ", t.id " + dir + " LIMIT ? OFFSET ?",
            MAPPER, pageArgs.toArray());
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }

    public Optional<TransactionResponse> findByIdAndUser(long id, long userId) {
        return jdbc.query(SELECT + " WHERE t.id = ? AND a.user_id = ?", MAPPER, id, userId).stream().findFirst();
    }
}
