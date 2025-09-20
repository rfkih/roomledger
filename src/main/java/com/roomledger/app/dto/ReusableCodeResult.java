package com.roomledger.app.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ReusableCodeResult(
        String paymentRequestId,
        String referenceId,
        String channelCode,
        Kind kind,
        String codeValue,                 // VA number or QR string
        OffsetDateTime expiresAt,
        Map<String, Object> raw           // full response (keep for persistence/audit)
) {
    public enum Kind { VIRTUAL_ACCOUNT, QR }
    public boolean isVa() { return kind == Kind.VIRTUAL_ACCOUNT; }
    public boolean isQr() { return kind == Kind.QR; }
}


