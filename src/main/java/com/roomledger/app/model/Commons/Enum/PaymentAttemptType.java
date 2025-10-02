package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentAttemptType implements CodedEnum {
    PAY("PAY", "One-off payment"),
    REUSABLE_PAYMENT_CODE("REUSABLE_PAYMENT_CODE", "Reusable payment code");

    private final String code;
    private final String description;

    PaymentAttemptType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static PaymentAttemptType fromCode(String code) {
        return EnumUtils.fromCode(PaymentAttemptType.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentAttemptType fromJson(String code) {
        return fromCode(code);
    }
}
