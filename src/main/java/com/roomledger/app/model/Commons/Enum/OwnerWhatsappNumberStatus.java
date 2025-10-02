package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum OwnerWhatsappNumberStatus implements CodedEnum {
    ACTIVE("ACTIVE", "Number is active"),
    INACTIVE("INACTIVE", "Number is inactive");

    private final String code;
    private final String description;

    OwnerWhatsappNumberStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static OwnerWhatsappNumberStatus fromCode(String code) {
        return EnumUtils.fromCode(OwnerWhatsappNumberStatus.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OwnerWhatsappNumberStatus fromJson(String code) {
        return fromCode(code);
    }
}