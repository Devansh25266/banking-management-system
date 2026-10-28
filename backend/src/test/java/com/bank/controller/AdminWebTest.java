package com.bank.controller;

import com.bank.config.SecurityConfig;
import com.bank.model.BankUser;
import com.bank.service.AdminService;
import com.bank.service.BankUserDetailsService;
import com.bank.service.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminWebTest {
    @Autowired MockMvc mvc;
    @MockBean AdminService adminService;
    @MockBean TransactionQueryService transactionQueryService;
    @MockBean BankUserDetailsService userDetails;

    BankUser principalWithRole(String role) {
        return new BankUser(1, "U", "u@b.com", "x", "9876543210", "addr", role, true);
    }

    @Test void anonymousIs401() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }
    @Test void customerIs403OnEveryAdminEndpoint() throws Exception {
        for (String p : new String[]{"/api/admin/dashboard", "/api/admin/accounts", "/api/admin/transactions", "/api/admin/audit-logs"})
            mvc.perform(get(p).with(user(principalWithRole("CUSTOMER")))).andExpect(status().isForbidden());
    }
    @Test void adminCanReadDashboard() throws Exception {
        mvc.perform(get("/api/admin/dashboard").with(user(principalWithRole("ADMIN")))).andExpect(status().isOk());
    }
    @Test void statusChangeNeedsCsrfToken() throws Exception {
        mvc.perform(patch("/api/admin/accounts/5/status").with(user(principalWithRole("ADMIN"))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"FROZEN\"}")).andExpect(status().isForbidden());
    }
    @Test void adminCanChangeStatusAndInvalidStatusIs400() throws Exception {
        mvc.perform(patch("/api/admin/accounts/5/status").with(user(principalWithRole("ADMIN"))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FROZEN\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/accounts/5/status").with(user(principalWithRole("ADMIN"))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETED\"}")).andExpect(status().isBadRequest());
    }
    @Test void customerCannotChangeStatus() throws Exception {
        mvc.perform(patch("/api/admin/accounts/5/status").with(user(principalWithRole("CUSTOMER"))).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FROZEN\"}")).andExpect(status().isForbidden());
    }
}
