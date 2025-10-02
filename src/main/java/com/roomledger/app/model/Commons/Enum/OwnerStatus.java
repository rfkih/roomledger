package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OwnerStatus implements CodedEnum {
    ACTIVE("ACTIVE", "Owner is active"),
    INACTIVE("INACTIVE", "Owner is inactive");

    private final String code;
    private final String description;

    OwnerStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static OwnerStatus fromCode(String code) {
        return EnumUtils.fromCode(OwnerStatus.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OwnerStatus fromJson(String code) {
        return fromCode(code);
    }
}
