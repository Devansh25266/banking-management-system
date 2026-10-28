package com.bank.repository;

import com.bank.model.BankUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserRepository {
    private static final RowMapper<BankUser> MAPPER = (rs, i) -> new BankUser(rs.getLong("id"), rs.getString("full_name"),
        rs.getString("email"), rs.getString("password_hash"), rs.getString("phone"), rs.getString("address"),
        rs.getString("role"), "ACTIVE".equals(rs.getString("status")));
    private final JdbcTemplate jdbc;
    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActiveCustomer(long id) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ? AND role = 'CUSTOMER' AND status = 'ACTIVE'", Integer.class, id);
        return c != null && c > 0;
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return c != null && c > 0;
    }
    public Optional<BankUser> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE email = ?", MAPPER, email).stream().findFirst();
    }
    /** Always inserts role CUSTOMER; admins are provisioned separately. */
    public long insert(String fullName, String email, String passwordHash, String phone, String address) {
        var kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO users (full_name, email, password_hash, phone, address, role, status) VALUES (?,?,?,?,?,'CUSTOMER','ACTIVE')",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fullName); ps.setString(2, email); ps.setString(3, passwordHash);
            ps.setString(4, phone); ps.setString(5, address);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public int updateCustomerStatus(long id, String status) {
        return jdbc.update("UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND role = 'CUSTOMER'", status, id);
    }

    /** Used only by startup provisioning; there is no public endpoint that can create an administrator. */
    public void insertAdmin(String fullName, String email, String passwordHash, String phone, String address) {
        jdbc.update("INSERT INTO users (full_name, email, password_hash, phone, address, role, status) VALUES (?,?,?,?,?,'ADMIN','ACTIVE')",
            fullName, email, passwordHash, phone, address);
    }
}
