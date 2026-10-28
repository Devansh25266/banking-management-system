package com.bank.repository;

import com.bank.dto.AuditLogResponse;
import com.bank.dto.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AuditRepository {
    private static final RowMapper<AuditLogResponse> MAPPER = (rs, i) -> new AuditLogResponse(rs.getLong("id"),
        rs.getLong("admin_user_id"), rs.getString("email"), rs.getString("action"), rs.getString("entity_type"),
        rs.getLong("entity_id"), rs.getTimestamp("created_at").toLocalDateTime());
    private final JdbcTemplate jdbc;
    public AuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(long adminId, String action, String entityType, long entityId) {
        jdbc.update("INSERT INTO audit_logs (admin_user_id, action, entity_type, entity_id) VALUES (?,?,?,?)",
            adminId, action, entityType, entityId);
    }
    public PageResponse<AuditLogResponse> list(int page, int size) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
        List<AuditLogResponse> rows = jdbc.query("SELECT l.*, u.email FROM audit_logs l JOIN users u ON u.id = l.admin_user_id ORDER BY l.id DESC LIMIT ? OFFSET ?",
            MAPPER, size, (long) page * size);
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }
}
