package com.roomledger.app.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

public record ReusableCodeResult(
        String paymentRequestId,
        String referenceId,
        String channelCode,
        String kind,
        String codeValue,                 // VA number or QR string
        LocalDateTime expiresAt,
        Map<String, Object> raw
) {
}


