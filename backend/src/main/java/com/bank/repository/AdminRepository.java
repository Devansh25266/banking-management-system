package com.bank.repository;

import com.bank.dto.AdminAccountResponse;
import com.bank.dto.AdminCustomerResponse;
import com.bank.dto.DashboardResponse;
import com.bank.dto.PageResponse;
import com.bank.model.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class AdminRepository {
    private static final RowMapper<AdminAccountResponse> MAPPER = (rs, i) -> new AdminAccountResponse(rs.getLong("id"),
        rs.getString("account_number"), rs.getString("account_type"), rs.getBigDecimal("balance"), rs.getString("currency"),
        rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("full_name"), rs.getString("email"));
    private final JdbcTemplate jdbc;
    public AdminRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PageResponse<AdminAccountResponse> searchAccounts(String search, AccountStatus status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" AND (a.account_number LIKE ? OR u.email LIKE ? OR u.full_name LIKE ?)");
            args.add(like); args.add(like); args.add(like);
        }
        if (status != null) { where.append(" AND a.status = ?"); args.add(status.name()); }
        String from = " FROM accounts a JOIN users u ON u.id = a.user_id" + where;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size); pageArgs.add((long) page * size);
        List<AdminAccountResponse> rows = jdbc.query("SELECT a.*, u.full_name, u.email" + from + " ORDER BY a.id DESC LIMIT ? OFFSET ?",
            MAPPER, pageArgs.toArray());
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }

    public PageResponse<AdminCustomerResponse> searchCustomers(String search, String status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE role = 'CUSTOMER'");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" AND (full_name LIKE ? OR email LIKE ? OR phone LIKE ?)");
            args.add(like); args.add(like); args.add(like);
        }
        if (status != null && !status.isBlank()) { where.append(" AND status = ?"); args.add(status); }
        String from = " FROM users" + where;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args); pageArgs.add(size); pageArgs.add((long) page * size);
        List<AdminCustomerResponse> rows = jdbc.query("SELECT id, full_name, email, phone, address, status, created_at" + from + " ORDER BY id DESC LIMIT ? OFFSET ?",
            (rs, i) -> new AdminCustomerResponse(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"), rs.getString("phone"), rs.getString("address"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime()), pageArgs.toArray());
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }

    public DashboardResponse dashboard(int days) {
        Long customers = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'", Long.class);
        long active = 0, frozen = 0, closed = 0;
        for (Map<String, Object> m : jdbc.queryForList("SELECT status, COUNT(*) AS c FROM accounts GROUP BY status")) {
            long c = ((Number) m.get("c")).longValue();
            switch (String.valueOf(m.get("status"))) {
                case "ACTIVE" -> active = c;
                case "FROZEN" -> frozen = c;
                case "CLOSED" -> closed = c;
                default -> { }
            }
        }
        BigDecimal balances = jdbc.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM accounts", BigDecimal.class);
        long depCount = 0, wdCount = 0;
        BigDecimal depVol = BigDecimal.ZERO, wdVol = BigDecimal.ZERO;
        Timestamp since = Timestamp.valueOf(LocalDateTime.now().minusDays(days));
        for (Map<String, Object> m : jdbc.queryForList(
                "SELECT transaction_type AS t, COUNT(*) AS c, COALESCE(SUM(amount), 0) AS v FROM transactions WHERE created_at >= ? GROUP BY transaction_type", since)) {
            long c = ((Number) m.get("c")).longValue();
            BigDecimal v = (BigDecimal) m.get("v");
            if ("DEPOSIT".equals(m.get("t"))) { depCount = c; depVol = v; } else { wdCount = c; wdVol = v; }
        }
        return new DashboardResponse(days, customers == null ? 0 : customers, active + frozen + closed, active, frozen, closed,
            depCount, wdCount, depVol, wdVol, balances,
            "Sum of simulated balances for demonstration only; not real bank reserves.");
    }
}
