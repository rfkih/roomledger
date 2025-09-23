package com.roomledger.app.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record PaymentStartResult(
        UUID paymentId,
        String prId,
        String channelCode,
        String currency,
        long requestAmount,
        String vaNumber,          // non-null for VA
        String qrisQrString,      // non-null for QRIS
        LocalDateTime expiresAt,
        PaymentResponseDTO raw
) {}


