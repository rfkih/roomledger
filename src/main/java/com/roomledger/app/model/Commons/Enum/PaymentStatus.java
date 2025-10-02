package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus implements CodedEnum {
    PENDING("PENDING", "created locally, before calling Xendit"),
    WAITING_FOR_PAYMENT("WAITING_FOR_PAYMENT", "created after (Inquiry) Payment Committed, waiting customer action to pay"),
    PAID("PAID", "paid (from webhook)"),
    FAILED("FAILED", "failed (from webhook)"),
    EXPIRED("EXPIRED", "expired (from webhook)"),
    CANCELLED("CANCELLED", "cancelled"),
    VERIFIED("VERIFIED", "verified");

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) { this.code = code; this.description = description; }

    @Override @JsonValue
    public String getCode() { return code; }
    @Override public String getDescription() { return description; }

    public static PaymentStatus fromCode(String code) { return EnumUtils.fromCode(PaymentStatus.class, code); }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentStatus fromJson(String code) { return fromCode(code); }
}

