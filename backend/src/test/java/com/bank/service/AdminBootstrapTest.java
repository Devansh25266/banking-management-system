package com.bank.service;

import com.bank.config.AdminBootstrap;
import com.bank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {
    @Mock UserRepository users;
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test void createsAdminWithHashedPasswordWhenMissing() {
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        new AdminBootstrap(users, encoder, " Admin@Example.com ", "a-long-test-password").run(null);
        var hash = ArgumentCaptor.forClass(String.class);
        verify(users).insertAdmin(any(), eq("admin@example.com"), hash.capture(), any(), any());
        assertNotEquals("a-long-test-password", hash.getValue());
        assertTrue(encoder.matches("a-long-test-password", hash.getValue()));
    }
    @Test void neverTouchesAnExistingUser() {
        when(users.existsByEmail("admin@example.com")).thenReturn(true);
        new AdminBootstrap(users, encoder, "admin@example.com", "a-long-test-password").run(null);
        verify(users, never()).insertAdmin(any(), any(), any(), any(), any());
    }
    @Test void doesNothingWithoutConfiguration() {
        new AdminBootstrap(users, encoder, "", "").run(null);
        verifyNoInteractions(users);
    }
    @Test void rejectsShortPassword() {
        new AdminBootstrap(users, encoder, "admin@example.com", "short").run(null);
        verifyNoInteractions(users);
    }
}
