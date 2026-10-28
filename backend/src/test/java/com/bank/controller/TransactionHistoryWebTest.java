package com.bank.controller;

import com.bank.config.SecurityConfig;
import com.bank.model.BankUser;
import com.bank.service.BankUserDetailsService;
import com.bank.service.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionHistoryController.class)
@Import(SecurityConfig.class)
class TransactionHistoryWebTest {
    @Autowired MockMvc mvc;
    @MockBean TransactionQueryService service;
    @MockBean BankUserDetailsService userDetails;

    BankUser customer() { return new BankUser(1, "Asha", "a@b.com", "x", "9876543210", "addr", "CUSTOMER", true); }

    @Test void historyRequiresLogin() throws Exception {
        mvc.perform(get("/api/accounts/1/transactions")).andExpect(status().isUnauthorized());
    }
    @Test void unknownTransactionTypeIs400() throws Exception {
        mvc.perform(get("/api/accounts/1/transactions?type=REFUND").with(user(customer()))).andExpect(status().isBadRequest());
    }
    @Test void malformedDateIs400() throws Exception {
        mvc.perform(get("/api/accounts/1/transactions?from=yesterday").with(user(customer()))).andExpect(status().isBadRequest());
    }
}
