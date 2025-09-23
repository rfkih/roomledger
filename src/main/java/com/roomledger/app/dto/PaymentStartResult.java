package com.roomledger.app.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentStartResult(
        UUID bookingId,
        String prId,                 // gateway paymentRequestId
        String channelCode,
        String currency,             // "IDR"
        long requestAmount,
        LocalDateTime expiresAt,
        List<PaymentStartItem> items
) {}

