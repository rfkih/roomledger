package com.roomledger.app.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InquiryPaymentInfo(
        UUID paymentId,
        String type,
        long amount,
        String status,
        String channelCode,
        String vaNumber,
        String qrisQrString,
        LocalDateTime expiresAt
) {}

