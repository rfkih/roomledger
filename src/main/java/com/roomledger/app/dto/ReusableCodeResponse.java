package com.roomledger.app.dto;


import java.time.OffsetDateTime;
import java.util.Map;

public record ReusableCodeResponse(
        String paymentRequestId,
        String referenceId,
        String channelCode,
        String kind,                // "VIRTUAL_ACCOUNT" | "QR"
        String codeValue,           // VA number or QR string
        OffsetDateTime expiresAt,
        Map<String, Object> raw     // full response if you want to persist/audit
) {
    public static ReusableCodeResponse from(ReusableCodeResult r) {
        return new ReusableCodeResponse(
                r.paymentRequestId(),
                r.referenceId(),
                r.channelCode(),
                r.kind() == null ? null : r.kind().name(),
                r.codeValue(),
                r.expiresAt(),
                r.raw()
        );
    }
}
