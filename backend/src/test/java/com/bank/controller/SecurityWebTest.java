package com.bank.controller;

import com.bank.config.SecurityConfig;
import com.bank.dto.UserResponse;
import com.bank.model.BankUser;
import com.bank.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, AccountController.class})
@Import(SecurityConfig.class)
class SecurityWebTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean AccountService accountService;
    @MockBean BankUserDetailsService userDetails;

    BankUser customer() {
        return new BankUser(1, "Asha", "asha@example.com", new BCryptPasswordEncoder().encode("Secret123"), "9876543210", "addr", "CUSTOMER", true);
    }

    @Test void unauthenticatedAccountListIs401() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }
    @Test void postWithoutCsrfTokenIs403() throws Exception {
        mvc.perform(post("/api/accounts").with(user(customer())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountType\":\"SAVINGS\"}")).andExpect(status().isForbidden());
    }
    @Test void customerCannotReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/accounts").with(user(customer()))).andExpect(status().isForbidden());
    }
    @Test void registerReturns201AndNeverExposesPasswordHash() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(1, "Asha", "asha@example.com", "9876543210", "addr", "CUSTOMER"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
            "{\"fullName\":\"Asha\",\"email\":\"asha@example.com\",\"phone\":\"9876543210\",\"address\":\"addr\",\"password\":\"Secret123\",\"confirmPassword\":\"Secret123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void invalidRegistrationReturns400WithMessage() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
            "{\"fullName\":\"\",\"email\":\"bad\",\"phone\":\"1\",\"address\":\"a\",\"password\":\"x\",\"confirmPassword\":\"x\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }
    @Test void wrongPasswordIs401() throws Exception {
        when(userDetails.loadUserByUsername("asha@example.com")).thenReturn(customer());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"asha@example.com\",\"password\":\"Wrong1234\"}")).andExpect(status().isUnauthorized());
    }
    @Test void loginCreatesSessionThatUnlocksAccounts() throws Exception {
        when(userDetails.loadUserByUsername("asha@example.com")).thenReturn(customer());
        when(accountService.list(1L)).thenReturn(List.of());
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"Asha@Example.com\",\"password\":\"Secret123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/accounts").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
    }
}
