package com.roomledger.app.dto;


import java.util.Map;

public record ReusableCodeResponse(
        String paymentRequestId,
        String referenceId,
        String channelCode,
        String kind,                // "VIRTUAL_ACCOUNT" | "QR"
        String codeValue,           // VA number or QR string
        java.time.LocalDateTime expiresAt,
        Map<String, Object> raw
) {
    public static ReusableCodeResponse from(ReusableCodeResult r) {
        return new ReusableCodeResponse(
                r.paymentRequestId(),
                r.referenceId(),
                r.channelCode(),
                r.kind() == null ? null : r.kind(),
                r.codeValue(),
                r.expiresAt(),
                r.raw()
        );
    }
}
