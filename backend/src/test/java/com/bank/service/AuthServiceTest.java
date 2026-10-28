package com.bank.service;

import com.bank.dto.RegisterRequest;
import com.bank.exception.BankException;
import com.bank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    AuthService service() { return new AuthService(users, encoder); }
    RegisterRequest req(String email, String pw, String confirm) {
        return new RegisterRequest("Asha Rao", email, "9876543210", "12 MG Road", pw, confirm);
    }

    @Test void registerStoresBcryptHashNotPlaintext() {
        when(users.existsByEmail("asha@example.com")).thenReturn(false);
        when(users.insert(any(), any(), any(), any(), any())).thenReturn(7L);

        var res = service().register(req("  Asha@Example.com ", "Secret123", "Secret123"));

        var hash = ArgumentCaptor.forClass(String.class);
        verify(users).insert(eq("Asha Rao"), eq("asha@example.com"), hash.capture(), any(), any());
        assertNotEquals("Secret123", hash.getValue());
        assertTrue(hash.getValue().startsWith("$2"));
        assertTrue(encoder.matches("Secret123", hash.getValue()));
        assertEquals("CUSTOMER", res.role());
        assertEquals(7L, res.id());
    }
    @Test void duplicateEmailRejectedWithConflict() {
        when(users.existsByEmail("asha@example.com")).thenReturn(true);
        var ex = assertThrows(BankException.class, () -> service().register(req("asha@example.com", "Secret123", "Secret123")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(users, never()).insert(any(), any(), any(), any(), any());
    }
    @Test void concurrentDuplicateInsertAlsoConflict() {
        when(users.existsByEmail(any())).thenReturn(false);
        when(users.insert(any(), any(), any(), any(), any())).thenThrow(new DuplicateKeyException("dup"));
        var ex = assertThrows(BankException.class, () -> service().register(req("a@b.com", "Secret123", "Secret123")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
    @Test void mismatchedPasswordsRejected() {
        var ex = assertThrows(BankException.class, () -> service().register(req("a@b.com", "Secret123", "Other1234")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(users);
    }
}
