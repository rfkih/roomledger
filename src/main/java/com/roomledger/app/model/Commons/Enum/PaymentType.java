package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentType implements CodedEnum {
    DEPOSIT("DEPOSIT", "Deposit for booking"),
    RENT("RENT", "Monthly rental payment"),
    FULL("FULL", "Full payment");

    private final String code;
    private final String description;

    PaymentType(String code, String description) { this.code = code; this.description = description; }

    @Override @JsonValue
    public String getCode() { return code; }
    @Override public String getDescription() { return description; }

    public static PaymentType fromCode(String code) { return EnumUtils.fromCode(PaymentType.class, code); }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentType fromJson(String code) { return fromCode(code); }
}
