package com.roomledger.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActiveBillingResponse(
        UUID paymentId,
        String type,               // DEPOSIT | RENT
        String status,             // PENDING
        BigDecimal amount,
        LocalDate periodMonth,     // yyyy-MM-01 for RENT, null for DEPOSIT
        LocalDateTime createdAt,

        UUID bookingId,
        String bookingStatus,      // DRAFT/ACTIVE/...
        UUID tenantId,
        String tenantName,
        String phone,
        UUID roomId,
        String roomNo
) {}
