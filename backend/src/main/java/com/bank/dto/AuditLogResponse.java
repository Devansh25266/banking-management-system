package com.bank.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(long id, long adminUserId, String adminEmail, String action, String entityType,
                               long entityId, LocalDateTime createdAt) {}
