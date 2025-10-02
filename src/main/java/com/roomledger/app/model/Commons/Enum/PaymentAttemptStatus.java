package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentAttemptStatus implements CodedEnum {
    PENDING("PENDING", "created locally"),
    WAITING_FOR_PAYMENT("WAITING_FOR_PAYMENT", "waiting for customer to pay"),
    PAID("PAID", "paid (webhook)"),
    FAILED("FAILED", "failed (webhook)"),
    EXPIRED("EXPIRED", "expired (webhook)"),
    SUCCEEDED("SUCCEEDED", "attempt succeeded");

    private final String code;
    private final String description;

    PaymentAttemptStatus(String code, String description) { this.code = code; this.description = description; }

    @Override @JsonValue public String getCode() { return code; }
    @Override public String getDescription() { return description; }

    public static PaymentAttemptStatus fromCode(String code) { return EnumUtils.fromCode(PaymentAttemptStatus.class, code); }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentAttemptStatus fromJson(String code) { return fromCode(code); }
}

