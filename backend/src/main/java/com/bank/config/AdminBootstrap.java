package com.bank.config;

import com.bank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates one administrator from ADMIN_EMAIL / ADMIN_PASSWORD. Never modifies an existing user. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set; no administrator provisioned.");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (password.length() < 12) {
            log.warn("ADMIN_PASSWORD is shorter than 12 characters; administrator not created.");
            return;
        }
        if (users.existsByEmail(normalized)) {
            log.info("A user with the configured administrator email already exists; leaving it untouched.");
            return;
        }
        try {
            users.insertAdmin("Administrator", normalized, encoder.encode(password), "0000000000", "N/A");
            log.info("Administrator account created for {}", normalized);
        } catch (DuplicateKeyException e) {
            log.info("Administrator was created concurrently; leaving it untouched.");
        }
    }
}
