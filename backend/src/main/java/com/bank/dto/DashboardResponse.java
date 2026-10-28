package com.bank.dto;

import java.math.BigDecimal;

public record DashboardResponse(int periodDays, long totalCustomers, long totalAccounts, long activeAccounts,
        long frozenAccounts, long closedAccounts, long depositCount, long withdrawalCount,
        BigDecimal depositVolume, BigDecimal withdrawalVolume, BigDecimal demoTotalBalance, String balanceNote) {}
